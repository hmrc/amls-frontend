package controllers.aboutyou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.aboutyou.{AboutYou, YourDetails}
import views.html.aboutyou._

import scala.concurrent.Future

trait YourDetailsController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[AboutYou](AboutYou.key) map {
        case Some(AboutYou(Some(data), _)) =>
          Ok(your_details(Form2[YourDetails](data), edit))
        case _ =>
          Ok(your_details(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[YourDetails](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(your_details(f, edit)))
        case ValidForm(_, data) =>
          for {
            aboutYou <- dataCacheConnector.fetch[AboutYou](AboutYou.key)
            _ <- dataCacheConnector.save[AboutYou](AboutYou.key,
              aboutYou.yourDetails(data)
            )
          } yield Redirect(routes.SummaryController.get())
      }
    }
  }
}

object YourDetailsController extends YourDetailsController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
