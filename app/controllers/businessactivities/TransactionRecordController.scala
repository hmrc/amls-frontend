package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, TransactionRecord}
import views.html.businessactivities._

import scala.concurrent.Future

trait TransactionRecordController extends BaseController {
  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form: Form2[TransactionRecord] = (for {
            businessActivities <- response
            transactionRecord <- businessActivities.transactionRecord
          } yield Form2[TransactionRecord](transactionRecord)).getOrElse(EmptyForm)
          Ok(customer_transaction_records(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[TransactionRecord](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(customer_transaction_records(f, edit)))
        case ValidForm(_, data) => {
          for {
            businessActivity <-
            dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivity.transactionRecord(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
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