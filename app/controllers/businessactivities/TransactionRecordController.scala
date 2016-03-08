package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, TransactionRecord}

import scala.concurrent.Future

trait TransactionRecordController extends BaseController {
  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key) map {
        case Some(BusinessActivities(_, _, _, _, Some(data), _, _, _, _, _)) =>
          Ok(views.html.customer_transaction_records(Form2[TransactionRecord](data), edit))
        case _ =>
          Ok(views.html.customer_transaction_records(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
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
            case true => Redirect(routes.WhatYouNeedController.get())
            case false => Redirect(routes.BusinessFranchiseController.get())
          }
        }
      }
  }
}

object TransactionRecordController extends TransactionRecordController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}