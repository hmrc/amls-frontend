package controllers.aboutyou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.auth.AmlsRegime
import forms.{ValidForm, EmptyForm, Form2, InvalidForm}
import models.aboutyou.{YourDetails, AboutYou}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait YourDetailsController extends FrontendController with Actions {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[YourDetails](AboutYou.key) map {
        case Some(data) => Ok(views.html.your_details(Form2[YourDetails](data), edit))
        case _ => Ok(views.html.your_details(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request => {
      Form2[YourDetails](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.your_details(f, edit)))
        case ValidForm(_, data) =>
          for {
            aboutYou <- dataCacheConnector.fetchDataShortLivedCache[AboutYou](AboutYou.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[AboutYou](AboutYou.key,
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
