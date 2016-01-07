package controllers.aboutyou

import config.AMLSAuthConnector
import config.AmlsPropertiesReader._
import connectors.DataCacheConnector
import controllers.auth.AmlsRegime
import models.{AboutYou, YourDetails}
import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.validation.TextValidator

import scala.concurrent.Future


trait YourDetailsController extends FrontendController with Actions {

  val dataCacheConnector: DataCacheConnector
  val cacheKey = "your-details"

  val yourDetailsForm = Form(mapping(
    "firstname" -> TextValidator.mandatoryText("err.titleNotEntered.first_name",
      "err.invalidLength", getIntFromProperty("validationMaxLengthFirstName")),
    "middlename" -> optional(TextValidator.mandatoryText("", "err.invalidLength",
      getIntFromProperty("validationMaxLengthFirstName"))),
    "lastname" -> TextValidator.mandatoryText("err.titleNotEntered.last_name", "err.invalidLength",
      getIntFromProperty("validationMaxLengthFirstName"))
  )(YourDetails.apply)(YourDetails.unapply))

  def get(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[AboutYou](AboutYou.key, user.user.oid) map {
        case Some(AboutYou(Some(data), _)) => Ok(views.html.your_details(yourDetailsForm.fill(data), edit))
        case _ => Ok(views.html.your_details(yourDetailsForm, edit))
      }
  }

  def post(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request =>
      yourDetailsForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(views.html.your_details(errors, edit))),
        details => {
          for {
            aboutYou <- dataCacheConnector.fetchDataShortLivedCache[AboutYou](AboutYou.key, authContext.user.oid)
            _ <- dataCacheConnector.saveDataShortLivedCache[AboutYou](AboutYou.key, authContext.user.oid,
              AboutYou.merge(aboutYou, details)
            )
          } yield Redirect(routes.SummaryController.get())
        }
      )
  }
}

object YourDetailsController extends YourDetailsController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
