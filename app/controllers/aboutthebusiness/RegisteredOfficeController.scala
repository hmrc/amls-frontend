package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{AmlsDataCacheConnector, DataCacheConnector}
import controllers.AMLSGenericController
import forms.AboutTheBusinessForms._
import models.{BusinessCustomerDetails, RegisteredOffice}
import play.api.mvc.{AnyContent, Request}
import services.BusinessCustomerService
import uk.gov.hmrc.play.frontend.auth.AuthContext

trait RegisteredOfficeController extends AMLSGenericController {

  def dataCacheConnector: DataCacheConnector

  def businessCustomerService: BusinessCustomerService

  private val CACHE_KEY = "registeredOffice"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) = {
    val reviewBusinessDetailsFuture = businessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails]
    val cachedDataFuture = dataCacheConnector.fetchDataShortLivedCache[RegisteredOffice](CACHE_KEY)
    for {
      reviewBusinessDetails: BusinessCustomerDetails <- reviewBusinessDetailsFuture
      cachedData: Option[RegisteredOffice] <- cachedDataFuture
    } yield {
      val fm = cachedData.fold { registeredOfficeForm} { registeredOfficeForm.fill }
      Ok(views.html.registered_office(fm, reviewBusinessDetails))

      /*
              registeredOfficeForm.bind(Map("registeredOfficeAddress.line_1" -> reviewBusinessDetails.businessAddress.line_1,
                "registeredOfficeAddress.line_2" -> reviewBusinessDetails.businessAddress.line_2,
                "registeredOfficeAddress.line_3" -> reviewBusinessDetails.businessAddress.line_3.getOrElse(""),
                "registeredOfficeAddress.line_4" -> reviewBusinessDetails.businessAddress.line_4.getOrElse(""),
                "registeredOfficeAddress.postcode" -> reviewBusinessDetails.businessAddress.postcode.getOrElse(""),
                "registeredOfficeAddress.country" -> reviewBusinessDetails.businessAddress.country
              ))
      */

    }
  }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) = {
    registeredOfficeForm.bindFromRequest().fold(
      errors => {
        val reviewBusinessDetailsFuture = businessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails]
        for (reviewBusinessDetails <- reviewBusinessDetailsFuture) yield {
          BadRequest(views.html.registered_office(errors, reviewBusinessDetails))
        }
      },
      registeredOffice => {
        dataCacheConnector.saveDataShortLivedCache[RegisteredOffice](CACHE_KEY, registeredOffice) map { _ =>
          NotImplemented("Not implemented")
        }
      })
  }
}

object RegisteredOfficeController extends RegisteredOfficeController {
  override def dataCacheConnector = AmlsDataCacheConnector

  override def authConnector = AMLSAuthConnector

  override def businessCustomerService = BusinessCustomerService
}