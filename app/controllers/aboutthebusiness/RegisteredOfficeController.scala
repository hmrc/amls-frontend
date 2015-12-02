package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import controllers.AMLSGenericController
import forms.AboutTheBusinessForms._
import models._
import play.api.i18n.{Lang, Messages}
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future
import scala.util.Success

trait RegisteredOfficeController extends AMLSGenericController {
  def optionsIsRegisteredOffice(implicit lang: Lang) = Seq(
    Messages("registeredoffice.lbl.yes.same") -> "1",
    Messages("registeredoffice.lbl.yes.different") -> "2",
    Messages("registeredoffice.lbl.no") -> "3"
  )

  def dataCacheConnector: DataCacheConnector

  def businessCustomerSessionCacheConnector: BusinessCustomerSessionCacheConnector

  private val CACHE_KEY = "registeredOffice"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) = {

    val reviewBusinessDetailsFuture: Future[BusinessCustomerDetails] = sessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
    val cachedDataFuture: Future[Option[RegisteredOfficeSave4Later]] = dataCacheConnector.fetchDataShortLivedCache[RegisteredOfficeSave4Later](CACHE_KEY)

    val eventualMaybeReviewBusinessDetails: Future[BusinessCustomerDetails] = for {
      reviewBusinessDetails <- reviewBusinessDetailsFuture
    } yield {
        reviewBusinessDetails
      }

    val eventualMaybeSave4Later: Future[Option[RegisteredOfficeSave4Later]] = for {
      cachedData <- cachedDataFuture
    } yield {
        cachedData
      }

    val eventualEventualTuple: Future[RegisteredOffice] = for {
      re <- eventualMaybeReviewBusinessDetails
      ch <- eventualMaybeSave4Later
      regoff <- Future { RegisteredOffice.fromRegisteredOfficeSave4Later(ch.get)}
      } yield {
        regoff
      }

    eventualEventualTuple.

    eventualEventualTuple onSuccess {
      case t: RegisteredOffice => t
    }

    Ok(views.html.registered_office(registeredOfficeForm.fill(t),null, optionsIsRegisteredOffice))



    /*
        val z: Future[RegisteredOffice] = eventualMaybeSave4Later flatMap {
          (x: Option[RegisteredOfficeSave4Later]) => Future(x) map {
            y => RegisteredOffice.fromRegisteredOfficeSave4Later(y.get)
          }
        }
    */

    /*
        val eventualBusinessDetails =  eventualMaybeReviewBusinessDetails.onComplete {
          case Success(businessDetails) => {Ok(views.html.registered_office(fm, eventualBusinessDetails, optionsIsRegisteredOffice))}
          case Failure(t) => throw new RuntimeException("eventual business details")
        }
    */


    /*
        val eventualOffice = z onComplete() {
          case Success(regOff) => Ok(views.html.registered_office(fm, eventualBusinessDetails, optionsIsRegisteredOffice))
          case Failure(t) => throw new RuntimeException("eventualOffice")
        }
    */


    /*
        val future: Future[Option[RegisteredOfficeSave4Later]] = for {
          reviewBusinessDetails <- reviewBusinessDetailsFuture
          cachedData <- cachedDataFuture
          } yield {
            cachedData
          }
    */

    //    val fm = cachedData.fold(registeredOfficeForm)(x => registeredOfficeForm.fill())

    //RegisteredOffice.fromRegisteredOfficeSave4Later(x)
    //future


  }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) = {
    val reviewBusinessDetailsFuture = businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
    registeredOfficeForm.bindFromRequest().fold(
      errors => {
        for (reviewBusinessDetails: BusinessCustomerDetails <- reviewBusinessDetailsFuture) yield {
          BadRequest(views.html.registered_office(errors, reviewBusinessDetails, optionsIsRegisteredOffice))
        }
      },
      regOffice => {
        val futureFutureResult: Future[Future[Result]] =
          for {
            reviewBusinessDetails: BusinessCustomerDetails <- reviewBusinessDetailsFuture
            result = dataCacheConnector.saveDataShortLivedCache[RegisteredOfficeSave4Later](CACHE_KEY,
              RegisteredOfficeSave4Later(reviewBusinessDetails.businessAddress,
                regOffice.isRegisteredOffice, regOffice.isCorrespondenceAddressSame))
          } yield {
            result map {
              case Some(RegisteredOfficeSave4Later(_, true, true)) => Redirect(controllers.aboutthebusiness.routes.TelephoningBusinessController.get())
              case _ => NotImplemented("Not yet implemented")
            }
          }
        futureFutureResult.flatMap({ (x: Future[Result]) => x map { (y: Result) => y } })
      }
    )
  }
}

object RegisteredOfficeController extends RegisteredOfficeController {
  override def dataCacheConnector = DataCacheConnector

  override def authConnector = AMLSAuthConnector

  override def businessCustomerSessionCacheConnector = BusinessCustomerSessionCacheConnector
}