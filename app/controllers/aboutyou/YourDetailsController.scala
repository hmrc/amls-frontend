package controllers.aboutyou

import config.AMLSAuthConnector
import config.AmlsPropertiesReader._
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import models.YourDetails
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.validation.TextValidator

import scala.concurrent.Future


trait YourDetailsController extends AMLSGenericController {

  protected val dataCacheConnector: DataCacheConnector
  protected val cacheKey = "your-details"

  val yourDetailsForm = Form(mapping(
    "firstname" -> TextValidator.mandatoryText("err.titleNotEntered.first_name",
      "err.invalidLength", getIntFromProperty("validationMaxLengthFirstName")),
    "middlename" -> optional(TextValidator.mandatoryText("", "err.invalidLength",
      getIntFromProperty("validationMaxLengthFirstName"))),
    "lastname" -> TextValidator.mandatoryText("err.titleNotEntered.last_name", "err.invalidLength",
      getIntFromProperty("validationMaxLengthFirstName"))
  )(YourDetails.apply)(YourDetails.unapply))

  override def get(implicit user: AuthContext, request: Request[AnyContent]) =
    dataCacheConnector.fetchDataShortLivedCache[YourDetails](cacheKey) map {
      case Some(data) => Ok(views.html.your_details(yourDetailsForm.fill(data)))
      case _ => Ok(views.html.your_details(yourDetailsForm))
    }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) =
    yourDetailsForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.your_details(errors))),
      details => {
        dataCacheConnector.saveDataShortLivedCache[YourDetails](cacheKey, details) map { _=>
          Redirect(controllers.aboutyou.routes.RoleWithinBusinessController.get())
        }
      })
}

object YourDetailsController extends YourDetailsController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
