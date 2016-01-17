package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.aboutthebusiness.{ConfirmRegisteredOfficeOrMainPlaceOfBusiness, BusinessCustomerDetails}

import scala.concurrent.Future

trait ConfirmRegisteredOfficeOrMainPlaceOfBusinessController extends BaseController  {

  def dataCacheConnector: DataCacheConnector
  def businessCustomerSessionCacheConnector: BusinessCustomerSessionCacheConnector

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      for {
        reviewBusinessDetails <- businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
      } yield {
        reviewBusinessDetails match {
          case Some(data) => Ok(views.html.confirm_registered_office_or_main_place(EmptyForm, data))
          case _ => Redirect(routes.RegisteredOfficeOrMainPlaceOfBusinessController.get())
        }
      }
  }

  def post() = Authorised.async {
    implicit authContext => implicit request =>

      Form2[ConfirmRegisteredOfficeOrMainPlaceOfBusiness](request.body) match {
        case f: InvalidForm => {
          for {
            reviewBusinessDetails <- businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
          } yield {
              reviewBusinessDetails match {
                case Some(data) => BadRequest(views.html.confirm_registered_office_or_main_place(f, data))
                case _ =>  Redirect(routes.ConfirmRegisteredOfficeOrMainPlaceOfBusinessController.get())
              }
            }
        }
        case ValidForm(_, data) => {
          data.isRegOfficeOrMainPlaceOfBusiness match {
            case true => Future.successful(Redirect(routes.BusinessRegisteredForVATController.get())) //TODO replace with correct path
            case false => Future.successful(Redirect(routes.RegisteredOfficeOrMainPlaceOfBusinessController.get()))
          }
        }
      }
  }
}

object ConfirmRegisteredOfficeOrMainPlaceOfBusinessController extends ConfirmRegisteredOfficeOrMainPlaceOfBusinessController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val businessCustomerSessionCacheConnector = BusinessCustomerSessionCacheConnector
}
