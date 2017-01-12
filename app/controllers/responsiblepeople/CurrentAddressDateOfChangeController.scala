package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.DateOfChange
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, ThreeYearsPlus}
import models.responsiblepeople.{ResponsiblePersonAddressHistory, ResponsiblePeople, ResponsiblePersonCurrentAddress}
import play.api.mvc.{AnyContent, Request}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.RepeatingSection
import views.html.responsiblepeople.current_address

import scala.concurrent.Future

trait CurrentAddressDateOfChangeController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  def get(index: Int, edit: Boolean) = Authorised {
    implicit authContext => implicit request =>
      Ok
  }

  def post(index: Int, edit: Boolean) = Authorised.async {
    implicit authContext => implicit request =>
      (Form2[DateOfChange](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(current_address(f, true, index)))
        case ValidForm(_, dateOfChange) =>

          doUpdate(index, dateOfChange)
          Future.successful(Redirect(routes.DetailedAnswersController.get(index)))
      }).recoverWith {
        case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
      }
  }


  private def doUpdate
  (index: Int, date: DateOfChange)
  (implicit authContext: AuthContext, request: Request[AnyContent]) = {
      updateDataStrict[ResponsiblePeople](index) { res =>

        for {
          addressHist <- res.addressHistory
          rpCurrentAdd <- addressHist.currentAddress
        } yield {
          val currentWDateOfChange = rpCurrentAdd.copy(dateOfChange = Some(date))
          val addHistWDateOfChange = addressHist.copy(currentAddress = Some(currentWDateOfChange))
          res.copy(addressHistory = Some(addHistWDateOfChange))
        }
      }
    }

}

object CurrentAddressDateOfChangeController extends CurrentAddressDateOfChangeController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
}