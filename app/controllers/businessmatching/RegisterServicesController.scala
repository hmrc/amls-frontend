package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.businessmatching.{MoneyServiceBusiness, BusinessMatching, BusinessActivities}
import scala.concurrent.Future
import views.html.businessmatching._

trait RegisterServicesController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map {
        response =>
          val form: Form2[BusinessActivities] = (for {
            businessMatching <- response
            businessActivities <- businessMatching.activities
          } yield Form2[BusinessActivities](businessActivities)).getOrElse(EmptyForm)
          Ok(register_services(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      import play.api.data.mapping.forms.Rules._
      Form2[BusinessActivities](request.body) match {
        case invalidForm: InvalidForm =>
          Future.successful(BadRequest(register_services(invalidForm, edit)))
        case ValidForm(_, data) =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
            _ <- dataCacheConnector.save[BusinessMatching](BusinessMatching.key,
              businessMatching.copy(activities = Some(data), msbServices = None)
            )
          } yield data.businessActivities.contains(MoneyServiceBusiness) match {
            case true => Redirect(routes.ServicesController.get(true))
            case false => Redirect(routes.SummaryController.get())
          }
      }
  }
}

object RegisterServicesController extends RegisterServicesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
