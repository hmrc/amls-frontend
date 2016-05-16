package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.aboutthebusiness.{ActivityStartDate, AboutTheBusiness}
import views.html.aboutthebusiness.activity_start_date

import scala.concurrent.Future

trait ActivityStartDateController extends BaseController {
   def dataCache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        response =>
          val form: Form2[ActivityStartDate] = (for {
            aboutTheBusiness <- response
            activityStartDate <- aboutTheBusiness.activityStartDate
          } yield Form2[ActivityStartDate](activityStartDate)).getOrElse(EmptyForm)
          Ok(activity_start_date(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ActivityStartDate](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(activity_start_date(f, edit)))
        case ValidForm(_, data) => {
          for {
            aboutTheBusiness <-
            dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCache.save[AboutTheBusiness](AboutTheBusiness.key,
              aboutTheBusiness.activityStartDate(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.ContactingYouController.get(edit))
          }
        }
      }
  }
}

object ActivityStartDateController extends ActivityStartDateController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
