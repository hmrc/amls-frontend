package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.businessactivities.{CustomersOutsideUK, BusinessActivities}
import utils.RepeatingSection

import scala.concurrent.Future

trait CustomersOutsideUKController extends RepeatingSection with BaseController {
  val dataCacheConnector: DataCacheConnector


  def get(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key) map {
        case Some(BusinessActivities(_ , _, _, _, _, Some(data), _)) =>
          Ok(views.html.customers_outside_uk(Form2[CustomersOutsideUK](data), edit))
        case _ =>
          Ok(views.html.customers_outside_uk(EmptyForm, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
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
            case false => Redirect(routes.BusinessFranchiseController.get())
          }
        }
      }
  }
}

object CustomersOutsideUKController extends CustomersOutsideUKController {
    override val authConnector = AMLSAuthConnector
    override val dataCacheConnector = DataCacheConnector
}