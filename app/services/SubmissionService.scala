package services

import config.ApplicationConfig
import connectors.{AmlsConnector, DataCacheConnector, GovernmentGatewayConnector}
import exceptions.NoEnrolmentException
import models.asp.Asp
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.responsiblepeople.ResponsiblePeople
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.{AmendVariationResponse, SubmissionResponse, SubscriptionRequest, SubscriptionResponse}
import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.{BusinessMatching, BusinessType}
import models.confirmation.{BreakdownRow, Currency}
import models.declaration.AddPerson
import models.estateagentbusiness.EstateAgentBusiness
import models.tradingpremises.TradingPremises
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.StatusConstants

import scala.concurrent.{ExecutionContext, Future}

trait SubmissionService extends DataCacheService {

  private[services] def cacheConnector: DataCacheConnector

  private[services] def amlsConnector: AmlsConnector

  private[services] def ggService: GovernmentGatewayService

  private[services] def authEnrolmentsService: AuthEnrolmentsService

  private object Submission {
    val message = "confirmation.submission"
    val quantity = 1
    val feePer = ApplicationConfig.regFee
  }

  private object Premises {
    val message = "confirmation.tradingpremises"
    val feePer = ApplicationConfig.premisesFee
  }


  private object People {
    val message = "confirmation.responsiblepeople"
    val feePer = ApplicationConfig.peopleFee
  }


  private object UnpaidPeople {
    val message = "confirmation.unpaidpeople"
    val feePer = 0
  }

  private def safeId(cache: CacheMap): Future[String] = {
    (for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      rd <- bm.reviewDetails
    } yield rd.safeId) match {
      case Some(a) =>
        Future.successful(a)
      case _ =>
        // TODO: Better exception
        Future.failed(new Exception(""))
    }
  }

  private def businessType(cache: CacheMap): Option[BusinessType] =
    for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      rd <- bm.reviewDetails
      bt <- rd.businessType
    } yield bt

  def bankDetailsExceptRemoved(bankDetails: Option[Seq[BankDetails]]): Option[Seq[BankDetails]] = {
    bankDetails match {
      case Some(bankAccts) => Some(bankAccts.filterNot(_.status.contains(StatusConstants.Deleted)))
      case _ => None
    }
  }

  private def createSubscriptionRequest
  (cache: CacheMap)
  (implicit
   ac: AuthContext,
   hc: HeaderCarrier,
   ec: ExecutionContext
  ): SubscriptionRequest = {
    SubscriptionRequest(
      businessMatchingSection = cache.getEntry[BusinessMatching](BusinessMatching.key),
      eabSection = cache.getEntry[EstateAgentBusiness](EstateAgentBusiness.key),
      tradingPremisesSection = cache.getEntry[Seq[TradingPremises]](TradingPremises.key),
      aboutTheBusinessSection = cache.getEntry[AboutTheBusiness](AboutTheBusiness.key),
      bankDetailsSection = bankDetailsExceptRemoved(cache.getEntry[Seq[BankDetails]](BankDetails.key)),
      aboutYouSection = cache.getEntry[AddPerson](AddPerson.key),
      businessActivitiesSection = cache.getEntry[BusinessActivities](BusinessActivities.key),
      responsiblePeopleSection = cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key),
      tcspSection = cache.getEntry[Tcsp](Tcsp.key),
      aspSection = cache.getEntry[Asp](Asp.key),
      msbSection = cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key),
      hvdSection = cache.getEntry[Hvd](Hvd.key),
      supervisionSection = cache.getEntry[Supervision](Supervision.key)
    )

  }

  def subscribe
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[SubscriptionResponse] = {
    for {
      cache <- getCache
      safeId <- safeId(cache)
      subscription <- amlsConnector.subscribe(createSubscriptionRequest(cache), safeId)
      _ <- cacheConnector.save[SubscriptionResponse](SubscriptionResponse.key, subscription)
      _ <- ggService.enrol(
        safeId = safeId,
        mlrRefNo = subscription.amlsRefNo
      )
    } yield subscription
  }

  def update
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[AmendVariationResponse] = {
    for {
      cache <- getCache
      regNo <- authEnrolmentsService.amlsRegistrationNumber
      amendment <- amlsConnector.update(
        createSubscriptionRequest(cache),
        regNo.getOrElse(throw new NoEnrolmentException("[SubmissionService][update] - No enrolment"))
      )
//      _ <- cacheConnector.save[AmendVariationResponse](AmendVariationResponse.key, amendment) recover{
//        case e:Throwable => println(" >>>> " + e.getMessage); throw new NoEnrolmentException("[SubmissionService][update] - No enrolment")
//      }
    } yield amendment
  }

  def getAmendment
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[(String, Currency, Seq[BreakdownRow], Option[Currency])] = {
    cacheConnector.fetchAll flatMap {
      option =>
        (for {
          cache <- option
          amendment <- cache.getEntry[AmendVariationResponse](AmendVariationResponse.key)
          premises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
          people <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
        } yield {
          val subQuantity = subscriptionQuantity(amendment)
          val mlrRegNo = "amlsReg"
          val total = amendment.totalFees
          val difference = amendment.difference map Currency.fromBD
          val rows = Seq(
            BreakdownRow(Submission.message, subQuantity, Submission.feePer, subQuantity * Submission.feePer)
          ) ++ responsiblePeopleRows(people, amendment) ++
            Seq(BreakdownRow(Premises.message, premises.size, Premises.feePer, amendment.premiseFee))
          Future.successful((mlrRegNo, Currency.fromBD(total), rows, difference))
        }) getOrElse Future.failed(new Exception("TODO"))
    }
  }

  private def subscriptionQuantity(subscription: SubmissionResponse): Int =
    if (subscription.registrationFee == 0) 0 else 1

  private def responsiblePeopleRows(people: Seq[ResponsiblePeople], subscription: SubmissionResponse): Seq[BreakdownRow] = {
    people.partition(_.hasAlreadyPassedFitAndProper.getOrElse(false)) match {
      case (b, a) =>
        Seq(BreakdownRow(People.message, a.size, People.feePer, Currency.fromBD(subscription.fpFee.getOrElse(0)))) ++
          (if (b.nonEmpty) {
            Seq(BreakdownRow(UnpaidPeople.message, b.size, UnpaidPeople.feePer, Currency.fromBD(UnpaidPeople.feePer)))
          } else {
            Seq.empty
          })
    }
  }

  def getSubscription
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[(String, Currency, Seq[BreakdownRow])] =
    cacheConnector.fetchAll flatMap {
      option =>
        (for {
          cache <- option
          subscription <- cache.getEntry[SubscriptionResponse](SubscriptionResponse.key)
          premises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
          people <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
        } yield {
          val subQuantity = subscriptionQuantity(subscription)
          val mlrRegNo = subscription.amlsRefNo
          val total = subscription.totalFees
          val rows = Seq(
            BreakdownRow(Submission.message, subQuantity, Submission.feePer, subQuantity * Submission.feePer)
          ) ++ responsiblePeopleRows(people, subscription) ++
            Seq(BreakdownRow(Premises.message, premises.size, Premises.feePer, subscription.premiseFee))
          Future.successful((mlrRegNo, Currency.fromBD(total), rows))
          // TODO
        }) getOrElse Future.failed(new Exception("TODO"))
    }
}

object SubmissionService extends SubmissionService {

  object MockGGService extends GovernmentGatewayService {

    import play.api.http.Status.OK

    override private[services] def ggConnector: GovernmentGatewayConnector = GovernmentGatewayConnector

    override def enrol
    (mlrRefNo: String, safeId: String)
    (implicit
     hc: HeaderCarrier,
     ec: ExecutionContext
    ): Future[HttpResponse] = Future.successful(HttpResponse(OK))
  }

  override private[services] val cacheConnector = DataCacheConnector
  override private[services] val amlsConnector = AmlsConnector
  override private[services] val authEnrolmentsService = AuthEnrolmentsService
  override private[services] val ggService = {
    if (ApplicationConfig.enrolmentToggle) {
      GovernmentGatewayService
    } else {
      MockGGService
    }
  }
}
