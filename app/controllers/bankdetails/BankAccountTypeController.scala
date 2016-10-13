package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.bankdetails.{BankAccountType, BankDetails}
import models.status.{NotCompleted, SubmissionReady}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{RepeatingSection, StatusConstants}

import scala.concurrent.Future

trait BankAccountTypeController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        bankDetail <- getData[BankDetails](index)
        count <- getData[BankDetails].map(x => x.count(!_.status.contains(StatusConstants.Deleted)))
        allowedToEdit <- allowedToEdit(edit)
      } yield bankDetail match {
        case Some(BankDetails(Some(data), _, _,_)) if allowedToEdit =>
          Ok(views.html.bankdetails.bank_account_types(Form2[Option[BankAccountType]](Some(data)), edit, index, count))
        case Some(_) if allowedToEdit =>
          Ok(views.html.bankdetails.bank_account_types(EmptyForm, edit, index, count))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, count: Int = 0) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[Option[BankAccountType]](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.bankdetails.bank_account_types(f, edit, index, count)))
        case ValidForm(_, data) => {
          for {
            result <- updateDataStrict[BankDetails](index) { bd =>
              bd.bankAccountType(data)
            }
          } yield {
            data match {
              case Some(_) => Redirect(routes.BankAccountController.get(index, edit))
              case _ => Redirect(routes.SummaryController.get(false))
            }
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
    }
  }

  private def allowedToEdit(edit: Boolean)(implicit hc: HeaderCarrier, auth: AuthContext): Future[Boolean] = {
    statusService.getStatus map {
      case SubmissionReady | NotCompleted => true
      case _ => !edit
    }
  }
}

object BankAccountTypeController extends BankAccountTypeController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override val statusService = StatusService
}
