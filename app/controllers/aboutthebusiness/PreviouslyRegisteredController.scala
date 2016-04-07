package controllers.aboutthebusiness

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.aboutthebusiness.{AboutTheBusiness, PreviouslyRegistered}
import views.html.aboutthebusiness._

import scala.concurrent.Future

trait PreviouslyRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        case Some(AboutTheBusiness(Some(data), _, _, _, _)) =>
          Ok(previously_registered(Form2[PreviouslyRegistered](data), edit))
        case _ =>
          Ok(previously_registered(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[PreviouslyRegistered](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(previously_registered(f, edit)))
        case ValidForm(_, data) =>
          for {
            aboutTheBusiness <- dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCacheConnector.save[AboutTheBusiness](AboutTheBusiness.key,
              aboutTheBusiness.previouslyRegistered(data)
            )
          } yield edit match {
             case true => Redirect(routes.SummaryController.get())
             case false => Redirect(routes.VATRegisteredController.get(edit))
          }
      }
    }
  }
}

object PreviouslyRegisteredController extends PreviouslyRegisteredController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}