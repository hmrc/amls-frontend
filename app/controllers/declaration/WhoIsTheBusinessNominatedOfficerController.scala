package controllers.declaration

import config.AMLSAuthConnector
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.declaration.BusinessNominatedOfficer
import models.responsiblepeople.{NominatedOfficer, Positions, ResponsiblePeople}
import models.status.SubmissionReadyForReview
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.StatusConstants
import views.html.declaration.select_business_nominated_officer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait WhoIsTheBusinessNominatedOfficerController extends BaseController {

  private[controllers] def amlsConnector: AmlsConnector

  private[controllers] def dataCacheConnector: DataCacheConnector

  private[controllers] def statusService: StatusService

  def businessNominatedOfficerView (status: Status, form: Form2[BusinessNominatedOfficer], rp: Seq[ResponsiblePeople])
                                   (implicit auth: AuthContext, request: Request[AnyContent]): Future[Result] = {
    statusService.getStatus map {
      case SubmissionReadyForReview => status(select_business_nominated_officer("submit.amendment.application", form, rp))
      case _ => status(select_business_nominated_officer("submit.registration", form, rp))
    }
  }

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchAll flatMap {
        optionalCache =>
          (for {
            cache <- optionalCache
            responsiblePeople <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
          } yield businessNominatedOfficerView(Ok, EmptyForm, responsiblePeople.filter(!_.status.contains(StatusConstants.Deleted)))
            ) getOrElse businessNominatedOfficerView(Ok, EmptyForm, Seq.empty)
      }
  }

  def updateNominatedOfficer(eventualMaybePeoples: Option[Seq[ResponsiblePeople]],
                             data: BusinessNominatedOfficer):Future[Option[Seq[ResponsiblePeople]]] = {
    eventualMaybePeoples match {
      case Some(rpSeq) => {
        val updatedList = rpSeq.filter(!_.status.contains(StatusConstants.Deleted)).map {responsiblePerson =>
          responsiblePerson.personName.exists(name => name.fullNameWithoutSpace.equals(data.value)) match {
            case true => {
              val position = responsiblePerson.positions.fold[Option[Positions]](None)(p => Some(Positions(p.positions. + (NominatedOfficer), p.startDate)))
              responsiblePerson.copy(positions = position)
            }
            case false => responsiblePerson
          }
        }
        Future.successful(Some(updatedList))
      }
      case _ => Future.successful(eventualMaybePeoples)
    }
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
       Form2[BusinessNominatedOfficer](request.body) match {
         case f:InvalidForm =>  dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) flatMap {
           case Some(data) => businessNominatedOfficerView(BadRequest, f, data.filter(!_.status.contains(StatusConstants.Deleted)))
           case None => businessNominatedOfficerView(BadRequest, f, Seq.empty)
         }
         case ValidForm(_, data) => {
             data.value match {
               case "-1" => Future.successful(Redirect(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(false)))
               case x if(x.nonEmpty) => for {
                 responsiblePeople <- dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key)
                 rp <-  updateNominatedOfficer(responsiblePeople, data)
                 _ <- dataCacheConnector.save(ResponsiblePeople.key, rp)
               } yield Redirect(routes.WhoIsRegisteringController.get())
           }
         }
       }
  }
}

object WhoIsTheBusinessNominatedOfficerController extends WhoIsTheBusinessNominatedOfficerController {
  // $COVERAGE-OFF$
  override private[controllers] val amlsConnector: AmlsConnector = AmlsConnector
  override private[controllers] val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected[controllers] val authConnector: AuthConnector = AMLSAuthConnector
  override private[controllers] val statusService: StatusService = StatusService
}
