package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, UkAccountantsAddress, WhoIsYourAccountant}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait WhoIsYourAccountantController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  //TODO: Joe - cannot seem to provide a default for UK/Non UK without providing defaults for other co-products
  private val defaultValues = WhoIsYourAccountant("", None, UkAccountantsAddress("","", None, None, ""))

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form = (for {
            businessActivities <- response
            whoIsYourAccountant <- businessActivities.whoIsYourAccountant
          } yield {
            Form2[WhoIsYourAccountant](whoIsYourAccountant)
          }).getOrElse(Form2(defaultValues))
          Ok(views.html.businessactivities.who_is_your_accountant(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[WhoIsYourAccountant](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.businessactivities.who_is_your_accountant(f, edit)))
        case ValidForm(_, data) => {
          for {
            businessActivity <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivity.whoIsYourAccountant(data)
            )
          } yield if (edit) {
            Redirect(routes.SummaryController.get())
          } else {
            Redirect(routes.TaxMattersController.get())
          }
        }
      }
  }
}

object WhoIsYourAccountantController extends WhoIsYourAccountantController {
  // $COVERAGE-OFF$
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
