package controllers.tradingpremises

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businesscustomer.{Address => BCAddress}
import models.businessmatching.BusinessMatching
import models.tradingpremises.{Address, ConfirmAddress, TradingPremises, YourTradingPremises}
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ConfirmAddressController @Inject()(override val messagesApi: MessagesApi,
                                         val dataCacheConnector: DataCacheConnector,
                                         val authConnector: AuthConnector)
  extends RepeatingSection with BaseController {

  def getAddress(businessMatching: Future[Option[BusinessMatching]]): Future[Option[BCAddress]] = {
    businessMatching map {
      case Some(bm) => bm.reviewDetails.fold[Option[BCAddress]](None)(r => Some(r.businessAddress))
      case _ => None
    }
  }

  def get(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getAddress(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)) map {
          case Some(address) => Ok(views.html.tradingpremises.confirm_address(EmptyForm, address, index))
          case None => Redirect(routes.WhereAreTradingPremisesController.get(index))
        }
  }

  def updateAddressFromBM(bmOpt: Option[BusinessMatching]) : Option[YourTradingPremises] = {
    bmOpt match {
      case Some(bm) => bm.reviewDetails.fold[Option[YourTradingPremises]](None)(r => Some(YourTradingPremises(r.businessName,
        Address(r.businessAddress.line_1,
          r.businessAddress.line_2,
          r.businessAddress.line_3,
          r.businessAddress.line_4,
          r.businessAddress.postcode.getOrElse("")))))
      case _ => None
    }
  }

  def post(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[ConfirmAddress](request.body) match {
          case f: InvalidForm =>
            getAddress(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)) map {
              case Some(addr) => BadRequest(views.html.tradingpremises.confirm_address(f, addr, index))
              case _ => Redirect(routes.WhereAreTradingPremisesController.get(index))
            }
          case ValidForm(_, data) =>
            data.confirmAddress match {
              case true => {
                  for {
                    _ <- fetchAllAndUpdateStrict[TradingPremises](index) { (cache, tp) =>
                      tp.copy(yourTradingPremises = updateAddressFromBM(cache.getEntry[BusinessMatching](BusinessMatching.key)))
                    }
                  } yield Redirect(routes.ActivityStartDateController.get(index))
              }
              case false => Future.successful(Redirect(routes.WhereAreTradingPremisesController.get(index)))
            }
        }
  }

}
