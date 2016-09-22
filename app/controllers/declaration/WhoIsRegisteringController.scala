package controllers.declaration

import config.AMLSAuthConnector
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.declaration._
import models.responsiblepeople.{PositionWithinBusiness, ResponsiblePeople}
import models.status._
import services.AuthEnrolmentsService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import views.html.declaration.who_is_registering

import scala.concurrent.Future

trait WhoIsRegisteringController extends BaseController {

  private[controllers] def amlsConnector: AmlsConnector
  def dataCacheConnector: DataCacheConnector
  def authEnrolmentsService: AuthEnrolmentsService

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchAll map {
        optionalCache =>
          (for {
            cache <- optionalCache
            responsiblePeople <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
          } yield {
            (for {
              whoIsRegistering <- cache.getEntry[WhoIsRegistering](WhoIsRegistering.key)
            } yield Ok(who_is_registering(Form2[WhoIsRegistering](whoIsRegistering), responsiblePeople)))
              .getOrElse(Ok(who_is_registering(EmptyForm, responsiblePeople)))
          }) getOrElse Ok(who_is_registering(EmptyForm, Seq.empty))
      }
  }

  def getAddPerson(whoIsRegistering: WhoIsRegistering, responsiblePeople: Seq[ResponsiblePeople]): Option[AddPerson] = {

    val rpOption = responsiblePeople.find(_.personName.exists(name => whoIsRegistering.person.equals(name.firstName.concat(name.lastName))))
    val rp: ResponsiblePeople = rpOption.getOrElse(None)

    rp.personName match {
      case Some(name) => Some(AddPerson(name.firstName, name.middleName, name.lastName,
        rp.positions.fold[Set[PositionWithinBusiness]](Set.empty)(x => x.positions)))
      case _ => None
    }

  }

  implicit def getPosition(positions: Set[PositionWithinBusiness]): RoleWithinBusiness = {
    import models.responsiblepeople._

    positions.head match {
      case BeneficialOwner => BeneficialShareholder
      case Director => models.declaration.Director
      case InternalAccountant => models.declaration.InternalAccountant
      case NominatedOfficer => models.declaration.NominatedOfficer
      case Partner => models.declaration.Partner
      case SoleProprietor => models.declaration.SoleProprietor
    }
  }

  def post = Authorised.async {
    implicit authContext => implicit request => {
      Form2[WhoIsRegistering](request.body) match {
        case f: InvalidForm =>
          dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) map {
            case Some(data) => BadRequest(who_is_registering(f, data))
            case None => BadRequest(who_is_registering(f, Seq.empty))
          }
        case ValidForm(_, data) =>
          dataCacheConnector.fetchAll flatMap {
            optionalCache =>
              (for {
                cache <- optionalCache
                responsiblePeople <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
              } yield {
                dataCacheConnector.save[WhoIsRegistering](WhoIsRegistering.key, data)
                data.person match {
                  case "-1" => {
                    Future.successful(Redirect(routes.AddPersonController.get()))
                  }
                  case _ => {
                    getAddPerson(data, responsiblePeople) map { addPerson =>
                      dataCacheConnector.save[AddPerson](AddPerson.key, addPerson)
                    }
                    redirectToDeclarationPage
                  }
                }
              }) getOrElse redirectToDeclarationPage
          }
      }
    }
  }

  private def redirectToDeclarationPage(implicit hc: HeaderCarrier, auth: AuthContext) = {
    getAMLSRegNo flatMap {
      case Some(amlsRegNo) => etmpStatus(amlsRegNo)(hc, auth) flatMap {
        case SubmissionReadyForReview =>
          Future.successful(Redirect(routes.DeclarationController.getWithAmendment()))
        case _ => Future.successful(Redirect(routes.DeclarationController.get()))
      }
      case None => Future.successful(Redirect(routes.DeclarationController.get()))
    }
  }

  private def getAMLSRegNo(implicit hc: HeaderCarrier, auth: AuthContext): Future[Option[String]] =
    authEnrolmentsService.amlsRegistrationNumber

  private def etmpStatus(amlsRefNumber: String)(implicit hc: HeaderCarrier, auth: AuthContext): Future[SubmissionStatus] = {
    amlsConnector.status(amlsRefNumber) map {
      response => response.formBundleStatus match {
        case "Pending" => SubmissionReadyForReview
        case "Approved" => SubmissionDecisionApproved
        case "Rejected" => SubmissionDecisionRejected
        case _ => NotCompleted
      }
    }
  }

}

object WhoIsRegisteringController extends WhoIsRegisteringController {
  // $COVERAGE-OFF$
  override private[controllers] val amlsConnector: AmlsConnector = AmlsConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override val authEnrolmentsService: AuthEnrolmentsService = AuthEnrolmentsService
}
