package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.bankdetails.{BankAccountType, BankDetails}
import models.businessactivities.{BusinessActivities, TransactionRecord}
import utils.RepeatingSection

import scala.concurrent.Future

trait TransactionRecordController extends BaseController {
  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form = (for {
            businessActivities <- response
            transactionRecord <- businessActivities.transactionRecord
          } yield Form2[TransactionRecord](transactionRecord)).getOrElse(EmptyForm)
          Ok(views.html.customer_transaction_records(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[TransactionRecord](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.customer_transaction_records(f, edit)))
        case ValidForm(_, data) => {
          for {
            businessActivity <-
            dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[BusinessActivities](BusinessActivities.key,
              businessActivity.transactionRecord(data)
            )
          } yield edit match {
            case true => Redirect(routes.IdentifySuspiciousActivityController.get())
            case false => Redirect(routes.IdentifySuspiciousActivityController.get())
          }
        }
      }
  }
}

object TransactionRecordController extends TransactionRecordController {
    override val authConnector = AMLSAuthConnector
    override val dataCacheConnector = DataCacheConnector
}