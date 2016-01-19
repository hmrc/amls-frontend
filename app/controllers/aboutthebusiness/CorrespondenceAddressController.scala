package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.aboutthebusiness.{CorrespondenceAddress, AboutTheBusiness}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait CorrespondenceAddressController extends BaseController {

  protected def dataConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key) map {
        case Some(AboutTheBusiness(_, _, _, _, Some(data))) =>
          Ok(views.html.business_correspondence_address(Form2[CorrespondenceAddress](data), edit))
        case _ =>
          Ok(views.html.business_correspondence_address(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CorrespondenceAddress](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.business_correspondence_address(f, edit)))
        case ValidForm(_, data) =>
          for {
            aboutTheBusiness <- dataConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataConnector.saveDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key,
              aboutTheBusiness.correspondenceAddress(data)
            )
            //TODO Redirect to summary
          } yield Ok("")
      }
    }
  }
}

object CorrespondenceAddressController extends CorrespondenceAddressController {
  override protected val dataConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}
