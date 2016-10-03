package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.bankdetails.BankDetails
import utils.{StatusConstants, RepeatingSection}

import scala.concurrent.Future

trait RemoveResponsiblePersonController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.responsiblepeople.remove_responsible_person()))
  }


//  def get(index: Int, complete: Boolean = false) = Authorised.async {
//    implicit authContext => implicit request =>
//      getData[BankDetails](index) map {
//        case Some(BankDetails(_, Some(bankAcct), _,_)) =>
//          Ok(views.html.bankdetails.remove_bank_details(EmptyForm, index, bankAcct.accountName, complete))
//        case _ => NotFound(notFoundView)
//      }
//  }
//
//  def remove(index: Int, complete: Boolean = false) = Authorised.async {
//    implicit authContext => implicit request => {
//      for {
//        rs <- updateDataStrict[BankDetails](index) { ba =>
//          ba.copy(status = Some(StatusConstants.Deleted), hasChanged = true)
//        }
//      } yield Redirect(routes.SummaryController.get(complete))
//    }
//  }
}

object RemoveResponsiblePersonController extends RemoveResponsiblePersonController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
