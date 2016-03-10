package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, CustomersOutsideUK}
import utils.RepeatingSection

import scala.concurrent.Future

trait CustomersOutsideUKController extends RepeatingSection with BaseController {
  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form = (for {
            businessActivities <- response
            customers <- businessActivities.customersOutsideUK
          } yield Form2[CustomersOutsideUK](customers)).getOrElse(EmptyForm)
          Ok(views.html.customers_outside_uk(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[CustomersOutsideUK](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.customers_outside_uk(f, edit)))
        case ValidForm(_, data) => {
          for {
            businessActivity <-
            dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[BusinessActivities](BusinessActivities.key,
              businessActivity.customersOutsideUK(data)
            )
          } yield edit match {
            case true => Redirect(routes.WhatYouNeedController.get())
            case false => Redirect(routes.TransactionRecordController.get())
          }
        }
      }
  }
}

object CustomersOutsideUKController extends CustomersOutsideUKController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}