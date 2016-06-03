package controllers.msb

import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.moneyservicebusiness.{FundsTransfer, MoneyServiceBusiness}
import views.html.msb._

import scala.concurrent.Future

trait FundsTransferController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>
          val form: Form2[FundsTransfer] = (for {
            moneyServiceBusiness <- response
            fundsTransfer <- moneyServiceBusiness.fundsTransfer
          } yield Form2[FundsTransfer](fundsTransfer)).getOrElse(EmptyForm)
          Ok(funds_transfer(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[FundsTransfer](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(funds_transfer(f, edit)))
        case ValidForm(_, data) =>
      }
  }
}