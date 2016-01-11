package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import controllers.AMLSGenericController
import forms.AboutTheBusinessForms._
import models.{BusinessCustomerDetails, ContactingYou}
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait ContactingYouController extends AMLSGenericController{

  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  val businessCustomerSessionCacheConnector: BusinessCustomerSessionCacheConnector
  val CACHE_KEY = "contactingYou"

  override def get(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = {
    val reviewBusinessDetailsFuture = businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
    val testData = dataCacheConnector.fetchDataShortLivedCache[ContactingYou](CACHE_KEY)
    for{
      result <- reviewBusinessDetailsFuture
      cachedData <- testData
    } yield {
      val form = cachedData.fold(contactingYouForm)(x => contactingYouForm.fill(x))
      Ok(views.html.contacting_you(form, result))
    }
  }

  override def post(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = {
    val reviewBusinessDetailsFuture = businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
    contactingYouForm.bindFromRequest().fold(
      errors =>{
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
  }
}

object ContactingYouController extends ContactingYouController {
   override val authConnector: AuthConnector = AMLSAuthConnector
   override val dataCacheConnector: DataCacheConnector = DataCacheConnector
   override val businessCustomerSessionCacheConnector = BusinessCustomerSessionCacheConnector
}

