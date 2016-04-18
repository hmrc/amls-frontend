package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.aboutthebusiness.{UKCorrespondenceAddress, CorrespondenceAddress, AboutTheBusiness}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.aboutthebusiness._

import scala.concurrent.Future

trait CorrespondenceAddressController extends BaseController {

  protected def dataConnector: DataCacheConnector
  private val initialiseWithUK = UKCorrespondenceAddress("","", "", "", None, None, "")

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        case Some(AboutTheBusiness(_, _, _, _, _, Some(data))) =>
          Ok(correspondence_address(Form2[CorrespondenceAddress](data), edit))
        case _ =>
          Ok(correspondence_address(Form2[CorrespondenceAddress](initialiseWithUK), edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CorrespondenceAddress](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(correspondence_address(f, edit)))
        case ValidForm(_, data) =>
          for {
            aboutTheBusiness <- dataConnector.fetch[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataConnector.save[AboutTheBusiness](AboutTheBusiness.key,
              aboutTheBusiness.correspondenceAddress(data)
            )
          } yield Redirect(routes.SummaryController.get())
      }
    }
  }
}

object CorrespondenceAddressController extends CorrespondenceAddressController {
  // $COVERAGE-OFF$
  override protected val dataConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}
