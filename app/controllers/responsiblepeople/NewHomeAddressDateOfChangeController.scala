package controllers.responsiblepeople

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{Path, ValidationError}
import models.responsiblepeople.{NewHomeAddress, NewHomeDateOfChange, ResponsiblePeople}
import org.joda.time.LocalDate
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.new_home_date_of_change

import scala.concurrent.Future

@Singleton
class NewHomeAddressDateOfChangeController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                     val authConnector: AuthConnector) extends RepeatingSection
  with BaseController {

  def get(index: Int) = {
    Authorised.async {
      implicit authContext =>
        implicit request =>
          dataCacheConnector.fetchAll flatMap {
            cacheMap =>
              (for {
                cache <- cacheMap
                rp <- getData[ResponsiblePeople](cache, index)
              } yield  cache.getEntry[NewHomeDateOfChange](NewHomeDateOfChange.key) match {
                case Some(dateOfChange) => Future.successful(Ok(new_home_date_of_change(Form2(dateOfChange),index, rp.personName.fold[String]("")(_.fullName))))
                case None => Future.successful(Ok(new_home_date_of_change(EmptyForm,index, rp.personName.fold[String]("")(_.fullName))))
              }).getOrElse(Future.successful(NotFound(notFoundView)))
          }
    }
  }

  def activityStartDateField(index: Int)(implicit authContext: AuthContext, request: Request[AnyContent]) = {
     getData[ResponsiblePeople](index) map  {x =>
      val startDate = x.fold[Option[LocalDate]](None)(_.positions.fold[Option[LocalDate]](None)(_.startDate))
      val personName = ControllerHelper.rpTitleName(x)
      (startDate, personName)
    }

  }

  def post(index: Int) = {
    Authorised.async {
      implicit authContext =>
        implicit request =>
          activityStartDateField(index) flatMap {
            case (Some(activityStartDate), personName) => {

              val extraFields = Map("activityStartDate" -> Seq(activityStartDate.toString("yyyy-MM-dd")))

              (Form2[NewHomeDateOfChange](request.body.asFormUrlEncoded.get ++ extraFields) match {
                case f: InvalidForm =>
                    Future.successful(BadRequest(new_home_date_of_change(f, index, personName)))
                case ValidForm(_, data) => {
                  for {
                    _ <- dataCacheConnector.save[NewHomeDateOfChange](NewHomeDateOfChange.key, data)
                  } yield Redirect(routes.CurrentAddressDateOfChangeController.get(index))
                }
              }).recoverWith {
                case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
              }
            }
            case _ => Future.successful(NotFound(notFoundView))
          }

    }
  }
}

