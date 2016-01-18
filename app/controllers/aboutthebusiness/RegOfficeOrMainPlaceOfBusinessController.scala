package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.aboutthebusiness.{RegOfficeOrMainPlaceOfBusiness, BusinessCustomerDetails}

import scala.concurrent.Future

trait RegOfficeOrMainPlaceOfBusinessController extends BaseController  {

  def dataCacheConnector: DataCacheConnector
  def businessCustomerSessionCacheConnector: BusinessCustomerSessionCacheConnector

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      for {
        reviewBusinessDetails <- businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
      } yield {
        reviewBusinessDetails match {
          case Some(data) => Ok(views.html.registered_office_or_main_place(EmptyForm, data))
          case _ => Redirect(routes.BusinessRegisteredWithHMRCBeforeController.get())  //TODo replace with actual registered address page
        }
      }
  }

  def post() = Authorised.async {
    implicit authContext => implicit request =>

      Form2[RegOfficeOrMainPlaceOfBusiness](request.body) match {
        case f: InvalidForm => {
          for {
            reviewBusinessDetails <- businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
          } yield {
              reviewBusinessDetails match {
                case Some(data) => BadRequest(views.html.registered_office_or_main_place(f, data))
                case _ =>  Redirect(routes.RegOfficeOrMainPlaceOfBusinessController.get())
              }
            }
        }
        case ValidForm(_, data) => {
          data.isRegOfficeOrMainPlaceOfBusiness match {
            case true => Future.successful(Redirect(routes.ContactingYouController.get()))
            case false => Future.successful(Redirect(routes.BusinessRegisteredWithHMRCBeforeController.get())) //TODO replace with correct path
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
