package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import controllers.auth.AmlsRegime
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.aboutthebusiness.{AboutTheBusiness, RegOfficeOrMainPlaceOfBusiness, BusinessCustomerDetails}
import models.aboutyou.AboutYou
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

trait RegOfficeOrMainPlaceOfBusinessController extends FrontendController with Actions {
  def dataCacheConnector: DataCacheConnector

  def businessCustomerSessionCacheConnector: BusinessCustomerSessionCacheConnector

  def get(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request =>
      for {
        reviewBusinessDetails <- businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
        cachedData <- dataCacheConnector.fetchDataShortLivedCache[RegOfficeOrMainPlaceOfBusiness](AboutYou.key)
      } yield {
        cachedData match {
          case Some(data) => Ok(views.html.registered_office_or_main_place(Form2[RegOfficeOrMainPlaceOfBusiness](data), reviewBusinessDetails))
          case _ => Ok(views.html.registered_office_or_main_place(EmptyForm, reviewBusinessDetails))
        }
      }
  }

  def post(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request =>

      val reviewBusinessDetailsFuture = businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]

      Form2[RegOfficeOrMainPlaceOfBusiness](request.body) match {
        case f: InvalidForm => {
          for (reviewBusinessDetails: BusinessCustomerDetails <- reviewBusinessDetailsFuture) yield {
            BadRequest(views.html.registered_office_or_main_place(f, reviewBusinessDetails))
          }
        }
        case ValidForm(_, data) =>
          for {
             aboutthebusiness <- dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key,
              aboutthebusiness.regOfficeOrMainPlaceOfBusiness(data))
          } yield {
            data.isRegOfficeOrMainPlaceOfBusiness match {
              case true => Redirect(routes.BusinessRegForVATController.get())   //TODO replace with correct path
              case false => Redirect(routes.BusinessRegisteredWithHMRCBeforeController.get()) //TODO replace with correct path
            }
          }
      }
  }
}

object RegOfficeOrMainPlaceOfBusinessController extends RegOfficeOrMainPlaceOfBusinessController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val businessCustomerSessionCacheConnector = BusinessCustomerSessionCacheConnector
}
