package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{AmlsDataCacheConnector, DataCacheConnector}
import controllers.AMLSGenericController
import forms.AboutTheBusinessForms._
import models.{BusinessCustomerDetails, RegisteredOffice}
import play.api.data.Form
import play.api.mvc.{AnyContent, Request}
import services.BusinessCustomerService
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

trait RegisteredOfficeController extends AMLSGenericController {

  def dataCacheConnector: DataCacheConnector

  def businessCustomerService = BusinessCustomerService

  private val CACHE_KEY = "registeredOffice"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) = {
    val reviewBusinessDetailsFuture = BusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails]
    val cachedDataFuture = dataCacheConnector.fetchDataShortLivedCache[RegisteredOffice](CACHE_KEY)
    for {
      reviewBusinessDetails: BusinessCustomerDetails <- reviewBusinessDetailsFuture
      cachedData: Option[RegisteredOffice] <- cachedDataFuture
    } yield {
      val fm = cachedData.fold{
        println("))))) I AM HERE")
        val cc = registeredOfficeForm.bind( Map( "registeredOfficeAddress.line_1" -> "Line 1 here",
          "registeredOfficeAddress.line_2" -> "Line 2 here",
          "registeredOfficeAddress.line_3" -> "Line 3 here",
          "registeredOfficeAddress.line_4" -> "Line 4 here",
          "registeredOfficeAddress.postcode" -> "Postcode here",
          "registeredOfficeAddress.country" -> "Country here"
        ) )
        println("We must get value here" + cc("registeredOfficeAddress.line_2").value.getOrElse("????"))
        println(cc("registeredOfficeAddress").value.getOrElse("*******"))
        cc
      }{ x=>registeredOfficeForm.fill(x) }

      Ok(views.html.registeredOffice(fm, reviewBusinessDetails))
    }
  }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) =
    registeredOfficeForm.bindFromRequest().fold(
      errors => {
        val reviewBusinessDetailsFuture = BusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails]
        for( reviewBusinessDetails <- reviewBusinessDetailsFuture ) yield {
          BadRequest(views.html.registeredOffice(errors, reviewBusinessDetails))
        }
      },
      registeredOffice => {
        dataCacheConnector.saveDataShortLivedCache[RegisteredOffice](CACHE_KEY, registeredOffice) map { _ =>
          NotImplemented
        }
      })
}

object RegisteredOfficeController extends RegisteredOfficeController {
  override def dataCacheConnector = AmlsDataCacheConnector

  override def authConnector = AMLSAuthConnector
}