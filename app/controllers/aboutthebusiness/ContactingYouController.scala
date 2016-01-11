package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import controllers.BaseController
import controllers.auth.AmlsRegime
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.{ContactingYouDetails, AboutTheBusiness, ContactingYou}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait ContactingYouController extends BaseController {

  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  val businessCustomerSessionCacheConnector: BusinessCustomerSessionCacheConnector

  def get(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request =>
      //val businessCustomerDetailsFuture = businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
      val aboutTheBusinessFuture = dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
      for {
        //businessCustomerDetails <- businessCustomerDetailsFuture
        aboutTheBusiness <- aboutTheBusinessFuture
      } yield aboutTheBusiness match {
        case Some(AboutTheBusiness(Some(data))) => Ok(views.html.contacting_you(Form2[ContactingYou](data), edit))
        case _ => Ok(views.html.contacting_you(EmptyForm, edit))
      }

  }

  /*
      val reviewBusinessDetailsFuture = businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
      val testData = dataCacheConnector.fetchDataShortLivedCache[ContactingYou](CACHE_KEY)
      for{
        result <- reviewBusinessDetailsFuture
        cachedData <- testData
      } yield {
        val form = cachedData.fold(contactingYouForm)(x => contactingYouForm.fill(x))
        Ok(views.html.contacting_you(form, result))
      }
  Future(Ok("Success"))
  */

  /*  def post(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = {
      val reviewBusinessDetailsFuture = businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
      contactingYouForm.bindFromRequest().fold(
        errors => {
          for (reviewBusinessDetails: BusinessCustomerDetails <- reviewBusinessDetailsFuture) yield {
            BadRequest(views.html.contacting_you(errors, reviewBusinessDetails))
          }
        },
        details => {
          dataCacheConnector.saveDataShortLivedCache[ContactingYou](CACHE_KEY, details) map { _ =>
            details.letterToThisAddress match {
              case true => NotImplemented("Not yet implemented:should go to summary page")
              case false => NotImplemented("Not yet implemented:should go to Address for letters relating to anti-money laundering supervision page")
            }
          }
        })
    Future(NotImplemented("Not yet implemented:should go to Address for letters relating to anti-money laundering supervision page"))
  }
  */

  def post(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {

    implicit authContext => implicit request => {
      Form2[ContactingYouDetails](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.contacting_you(f, edit))) //We will create Address here
        case ValidForm(_, data: ContactingYouDetails) =>
          for {
            aboutTheBusiness <- dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key, aboutTheBusiness.contactingYou(ContactingYou(data.email, data.phoneNumber, data.website))
            )
          } yield data.sendLettersToThisAddress match {
            case true =>  Redirect(routes.ContactingYouController.get())
            case false => Redirect(routes.ContactingYouController.get())
          } //TODO Not Yet Implemented
      }
    }
  }

}

object ContactingYouController extends ContactingYouController {
  override val authConnector: AuthConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val businessCustomerSessionCacheConnector = BusinessCustomerSessionCacheConnector
}

