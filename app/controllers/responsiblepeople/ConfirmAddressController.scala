package controllers.responsiblepeople

import javax.inject.Inject

import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.businesscustomer.{Address => BusinessCustomerAddress}
import models.businessmatching.BusinessMatching
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection
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

  def get(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getAddress(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)) map {
          case Some(address) => Ok(views.html.responsiblepeople.confirm_address(EmptyForm, address, index))
          case None => Redirect(routes.CurrentAddressController.get(index))
        }
  }

  def post(index: Int) = Authorised.async{
    ???
  }

}
