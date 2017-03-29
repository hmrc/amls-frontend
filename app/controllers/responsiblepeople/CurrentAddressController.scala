package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.TimeAtAddress._
import models.responsiblepeople._
import models.status.{SubmissionDecisionApproved, SubmissionStatus}
import play.api.mvc.{AnyContent, Request}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{ControllerHelper, DateOfChangeHelper, RepeatingSection}
import views.html.responsiblepeople.current_address

import scala.concurrent.Future

trait CurrentAddressController extends RepeatingSection with BaseController with DateOfChangeHelper {

  def dataCacheConnector: DataCacheConnector

  val statusService: StatusService

  final val DefaultAddressHistory = ResponsiblePersonCurrentAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>

        getData[ResponsiblePeople](index) map {
          case Some(ResponsiblePeople(Some(personName), _, _,
          Some(ResponsiblePersonAddressHistory(Some(currentAddress), _, _)), _,_, _, _, _, _, _, _, _, _, _))
          => Ok(current_address(Form2[ResponsiblePersonCurrentAddress](currentAddress), edit, index, fromDeclaration, personName.titleName))
          case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _,_))
          => Ok(current_address(Form2(DefaultAddressHistory), edit, index, fromDeclaration, personName.titleName))
          case _
          => NotFound(notFoundView)
        }
    }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        (Form2[ResponsiblePersonCurrentAddress](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePeople](index) map { rp =>
              BadRequest(current_address(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            getData[ResponsiblePeople](index) flatMap { responsiblePerson =>
              (for {
                rp <- responsiblePerson
                addressHistory <- rp.addressHistory
                currentAddress <- addressHistory.currentAddress
              } yield {
                val currentAddressWithTime = data.copy(timeAtAddress = currentAddress.timeAtAddress)
                updateAndRedirect(currentAddressWithTime, index, edit, fromDeclaration)
              }) getOrElse updateAndRedirect(data, index, edit, fromDeclaration)
            }
          }
        }).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
    }

  private def updateAndRedirect
  (data: ResponsiblePersonCurrentAddress, index: Int, edit: Boolean, fromDeclaration: Boolean)
  (implicit authContext: AuthContext, request: Request[AnyContent]) = {
    updateDataStrict[ResponsiblePeople](index) { res =>
      res.addressHistory(
        res.addressHistory match {
          case Some(a) => a.currentAddress(data)
          case _ => ResponsiblePersonAddressHistory(currentAddress = Some(data))
        })
    } map { _ =>
      if (edit) {
        Redirect(routes.DetailedAnswersController.get(index, edit))
      } else {
        Redirect(routes.TimeAtCurrentAddressController.get(index, edit, fromDeclaration))
      }
    }
  }
}

object CurrentAddressController extends CurrentAddressController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  val statusService = StatusService

  override def dataCacheConnector = DataCacheConnector
}
