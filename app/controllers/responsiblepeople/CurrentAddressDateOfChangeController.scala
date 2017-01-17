package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.DateOfChange
import models.responsiblepeople.ResponsiblePeople
import models.responsiblepeople.TimeAtAddress.{SixToElevenMonths, ZeroToFiveMonths}
import org.joda.time.LocalDate
import play.api.mvc.{AnyContent, Request}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.RepeatingSection

import scala.concurrent.Future

//noinspection ScalaStyle
trait CurrentAddressDateOfChangeController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  def get(index: Int, edit: Boolean) = Authorised {
    implicit authContext => implicit request =>
      Ok(views.html.date_of_change(
        Form2[DateOfChange](DateOfChange(LocalDate.now)),
        "summary.responsiblepeople",
        controllers.responsiblepeople.routes.CurrentAddressDateOfChangeController.post(index, edit)
      ))
  }

  def post(index: Int, edit: Boolean) = Authorised.async {
    implicit authContext => implicit request =>

      val extraFieldsFut = dataCacheConnector.fetch[ResponsiblePeople](ResponsiblePeople.key) map { rp =>
        val startDate = for {
          position <- rp.positions
          date <- position.startDate
        } yield {
          date
        }

        startDate match {
          case Some(date) => Map("activityStartDate" -> Seq(date.toString("yyyy-MM-dd")))
          case _ => Map()
        }
      }

      extraFieldsFut.flatMap { extraFields =>
        (Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ extraFields) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.date_of_change(
              f, "summary.responsiblepeople",
              controllers.responsiblepeople.routes.CurrentAddressDateOfChangeController.post(index, edit))
            ))
          case ValidForm(_, dateOfChange) => {

            val futOptTimeAtCurrent = getData[ResponsiblePeople](index) map { rp =>
              for {
                addHist <- rp.addressHistory
                rpCurr <- addHist.currentAddress
              } yield {
                rpCurr.timeAtAddress
              }
            }

            doUpdate(index, dateOfChange).flatMap { _ =>
              futOptTimeAtCurrent map {
                case Some(ZeroToFiveMonths) | Some(SixToElevenMonths) =>
                  Redirect(routes.AdditionalAddressController.get(index, edit))
                case Some(_) => Redirect(routes.DetailedAnswersController.get(index))
              }
            }
          }
        }).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  private def doUpdate
  (index: Int, date: DateOfChange)
  (implicit authContext: AuthContext, request: Request[AnyContent]) =

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

object CurrentAddressDateOfChangeController extends CurrentAddressDateOfChangeController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
}