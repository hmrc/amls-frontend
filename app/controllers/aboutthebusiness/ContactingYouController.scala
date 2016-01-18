package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.aboutthebusiness._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

trait ContactingYouController extends BaseController {

  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  val businessCustomerSessionCacheConnector: BusinessCustomerSessionCacheConnector


  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      val businessCustomerDetailsFuture = businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails] //TODO Remove after AboutTheBusiness has Address
      val aboutTheBusinessFuture = dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
      for {
        businessCustomerDetails <- businessCustomerDetailsFuture
        aboutTheBusiness <- aboutTheBusinessFuture
      } yield aboutTheBusiness match {
        case Some(AboutTheBusiness(_, _, Some(data))) => Ok(views.html.contacting_you(Form2[ContactingYou](data), businessCustomerDetails, edit))
        case _ => Ok(views.html.contacting_you(EmptyForm, businessCustomerDetails, edit))
      }
  }


  def post(edit: Boolean = false) = Authorised.async {

    implicit authContext => implicit request => {
      Form2[ContactingYouForm](request.body) match {
        case f: InvalidForm => {
          for {
            reviewBusinessDetails <- businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails] //TODO Get Address from AboutTheBusiness
          } yield {
            reviewBusinessDetails match {
              case Some(data) => BadRequest(views.html.contacting_you(f, Some(data), edit))
              case _ => Redirect(routes.ContactingYouController.get())
            }
          }
        }
        case ValidForm(_, data) =>
          for {
            aboutTheBusiness <- dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key, aboutTheBusiness.contactingYou(ContactingYou(data.email, data.phoneNumber, data.website))
            )
          } yield data.letterToThisAddress match {
            case true => Redirect(routes.ContactingYouController.get(edit)) //TODO Go to the Summary Page as no change in Address
            case false => Redirect(routes.ContactingYouController.get(edit)) //TODO  Go to the Address For Letters
          }
      }
    }
  }

}

object ContactingYouController extends ContactingYouController {
  override val authConnector: AuthConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val businessCustomerSessionCacheConnector = BusinessCustomerSessionCacheConnector
}
