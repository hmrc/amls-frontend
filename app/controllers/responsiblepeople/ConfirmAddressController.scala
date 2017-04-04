package controllers.responsiblepeople

import javax.inject.Inject

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.BusinessActivities
import models.businesscustomer.{Address => BusinessCustomerAddress}
import models.businessmatching.BusinessMatching
import models.responsiblepeople._
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.confirm_address

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class ConfirmAddressController @Inject()(override val messagesApi: MessagesApi,
                                         val dataCacheConnector: DataCacheConnector,
                                         val authConnector: AuthConnector)
  extends RepeatingSection with BaseController {

  def getAddress(businessMatching: Future[Option[BusinessMatching]]): Future[Option[BusinessCustomerAddress]] = {
    businessMatching map {
      case Some(bm) => bm.reviewDetails.fold[Option[BusinessCustomerAddress]](None)(r => Some(r.businessAddress))
      case _ => None
    }
  }
  def getAddress(businessMatching: BusinessMatching): Option[BusinessCustomerAddress] = {
      businessMatching.reviewDetails.fold[Option[BusinessCustomerAddress]](None)(r => Some(r.businessAddress))
  }

  def updateAddressFromBM(bmOpt: Option[BusinessMatching]) : Option[ResponsiblePersonAddressHistory] = {
    bmOpt match {
      case Some(bm) => bm.reviewDetails.fold[Option[ResponsiblePersonAddressHistory]](None)(r => {
        val UKAddress = PersonAddressUK(r.businessAddress.line_1,
          r.businessAddress.line_2,
          r.businessAddress.line_3,
          r.businessAddress.line_4,
          r.businessAddress.postcode.getOrElse(""))
        val additionalAddress = ResponsiblePersonAddress(UKAddress, None)
        Some(ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress)))
      }
      )
      case _ => None
    }
  }

  def get(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetchAll map {
          optionalCache =>
            (for {
              cache <- optionalCache
              rp <- getData[ResponsiblePeople](cache, index)
              bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            } yield {
              getAddress(bm) match {
                case Some(addr) => Ok(confirm_address(EmptyForm, addr, index, ControllerHelper.rpTitleName(Some(rp))))
                case _ => Redirect(routes.CurrentAddressController.get(index))
              }
            }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
        }
  }

  def post(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[ConfirmAddress](request.body) match {
          case f: InvalidForm =>
            dataCacheConnector.fetchAll flatMap {
              optionalCache =>
                (for {
                  cache <- optionalCache
                  rp <- getData[ResponsiblePeople](cache, index)
                  bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
                } yield {
                  getAddress(bm) match {
                    case Some(addr) => Future.successful(BadRequest(views.html.responsiblepeople.confirm_address(f, addr,
                      index, ControllerHelper.rpTitleName(Some(rp)))))
                    case _ => Future.successful(Redirect(routes.CurrentAddressController.get(index)))
                  }
                }) getOrElse Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
            }
          case ValidForm(_, data) =>
            data.confirmAddress match {
              case true => {
                for {
                  _ <- fetchAllAndUpdateStrict[ResponsiblePeople](index) { (cache, rp) =>
                    rp.copy(addressHistory = updateAddressFromBM(cache.getEntry[BusinessMatching](BusinessMatching.key)))
                  }
                } yield Redirect(routes.TimeAtCurrentAddressController.get(index))
              }
              case false => Future.successful(Redirect(routes.CurrentAddressController.get(index)))
            }
        }
  }

}
