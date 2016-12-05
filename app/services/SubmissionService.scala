package services

import config.ApplicationConfig
import connectors.{AmlsConnector, DataCacheConnector, GovernmentGatewayConnector}
import exceptions.NoEnrolmentException
import models.aboutthebusiness.AboutTheBusiness
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.{BusinessMatching, BusinessType}
import models.confirmation.{BreakdownRow, Currency}
import models.declaration.AddPerson
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.responsiblepeople.ResponsiblePeople
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import models.{AmendVariationResponse, SubmissionResponse, SubscriptionRequest, SubscriptionResponse}
import play.api.Logger
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
    val feePer: BigDecimal = ApplicationConfig.regFee
  }

  private object Premises {
    val message = "confirmation.tradingpremises"
    val feePer: BigDecimal = ApplicationConfig.premisesFee
  }

  private object PremisesHalfYear {
    val message = "confirmation.tradingpremises.half"
    val feePer: BigDecimal = Premises.feePer / 2
  }

  private object PremisesZero {
    val message = "confirmation.tradingpremises.zero"
    val feePer: BigDecimal = 0
  }

  private object People {
    val message = "confirmation.responsiblepeople"
    val feePer: BigDecimal = ApplicationConfig.peopleFee
  }

  private object UnpaidPeople {
    val message = "confirmation.unpaidpeople"
    val feePer: BigDecimal = 0 - ApplicationConfig.peopleFee
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
          val paymentReference = subscription.paymentReference
          val total = subscription.totalFees
          val rows = getBreakdownRows(subscription, premises, people, subQuantity)
          Future.successful((paymentReference, Currency.fromBD(total), rows))
          // TODO
        }) getOrElse Future.failed(new Exception("TODO"))
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
      bankDetailsSection = bankDetailsExceptDeleted(cache.getEntry[Seq[BankDetails]](BankDetails.key)),
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
      _ <- cacheConnector.save[AmendVariationResponse](AmendVariationResponse.key, amendment)
    } yield amendment
  }

  def variation
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[AmendVariationResponse] = {
    for {
      cache <- getCache
      regNo <- authEnrolmentsService.amlsRegistrationNumber
      amendment <- amlsConnector.variation(
        createSubscriptionRequest(cache),
        regNo.getOrElse(throw new NoEnrolmentException("[SubmissionService][variation] - No enrolment"))
      )
      _ <- cacheConnector.save[AmendVariationResponse](AmendVariationResponse.key, amendment)
    } yield amendment
  }

  def getAmendment
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[Option[(Option[String], Currency, Seq[BreakdownRow], Option[Currency])]] = {
    cacheConnector.fetchAll flatMap {
      getDataForAmendment(_) getOrElse Future.failed(new Exception("Cannot get amendment response"))
    }
  }

  private def getDataForAmendment(option: Option[CacheMap])(implicit authContent: AuthContext, hc: HeaderCarrier, ec: ExecutionContext) = {
    for {
      cache <- option
      amendment <- cache.getEntry[AmendVariationResponse](AmendVariationResponse.key)
      premises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
      people <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
    } yield {
      val subQuantity = subscriptionQuantity(amendment)
      val total = amendment.totalFees
      val difference = amendment.difference map Currency.fromBD
      val filteredPremises = premises.filter(!_.status.contains(StatusConstants.Deleted))
      val rows = getBreakdownRows(amendment, filteredPremises, people, subQuantity)
      val paymentRef = amendment.paymentReference
      Future.successful(Some((paymentRef, Currency.fromBD(total), rows, difference)))
    }
  }

  def getVariation
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[Option[(Option[String], Currency, Seq[BreakdownRow])]] = {
    cacheConnector.fetchAll flatMap {
      option =>
        (for {
          cache <- option
          variation <- cache.getEntry[AmendVariationResponse](AmendVariationResponse.key)
        } yield {
          val premisesFee: BigDecimal = getTotalPremisesFee(variation)
          val peopleFee: BigDecimal = getPeopleFee(variation)
          val fitAndProperDeduction: BigDecimal = getFitAndProperDeduction(variation)
          val totalFees: BigDecimal = peopleFee + fitAndProperDeduction + premisesFee
          val rows = getVariationBreakdown(variation, peopleFee)
          val paymentRef = variation.paymentReference
          Future.successful(Some((paymentRef, Currency(totalFees), rows)))
        }) getOrElse Future.failed(new Exception("Cannot get amendment response"))
    }
  }

  private def getVariationBreakdown(variation: AmendVariationResponse, peopleFee: BigDecimal): Seq[BreakdownRow] = {

    val breakdownRows = Seq()

    def rpRow: Seq[BreakdownRow] = {
      val rp = variation.addedResponsiblePeople
      val fp = variation.addedResponsiblePeopleFitAndProper
      if (rp > 0 || fp > 0) {
        breakdownRows ++ Seq(BreakdownRow(People.message, rp + fp, People.feePer, Currency(peopleFee)))
      } else {
        Seq()
      }
    }

    def fpRow: Seq[BreakdownRow] = {
      if (variation.addedResponsiblePeopleFitAndProper > 0) {
        breakdownRows ++ Seq(BreakdownRow(UnpaidPeople.message, variation.addedResponsiblePeopleFitAndProper, UnpaidPeople.feePer, Currency(getFitAndProperDeduction(variation))))
      } else {
        Seq()
      }
    }

    def tpFullYearRow: Seq[BreakdownRow] = {
      if (variation.addedFullYearTradingPremises > 0) {
        breakdownRows ++ Seq(BreakdownRow(Premises.message, variation.addedFullYearTradingPremises, Premises.feePer, Currency(getFullPremisesFee(variation))))
      } else {
        Seq()
      }
    }

    def tpHalfYearRow: Seq[BreakdownRow] = {
      if (variation.halfYearlyTradingPremises > 0) {
        breakdownRows ++ Seq(BreakdownRow(PremisesHalfYear.message, variation.halfYearlyTradingPremises, PremisesHalfYear.feePer, Currency(getHalfYearPremisesFee(variation))))
      } else {
        Seq()
      }
    }

    def tpZeroRow: Seq[BreakdownRow] = {
      if (variation.zeroRatedTradingPremises > 0) {
        breakdownRows ++ Seq(BreakdownRow(PremisesZero.message, variation.zeroRatedTradingPremises, PremisesZero.feePer, Currency(PremisesZero.feePer)))
      } else {
        Seq()
      }
    }

    rpRow ++ fpRow ++ tpZeroRow ++ tpHalfYearRow ++ tpFullYearRow

  }

  private def getTotalPremisesFee(variation: AmendVariationResponse): BigDecimal = {
    (Premises.feePer * variation.addedFullYearTradingPremises) + getHalfYearPremisesFee(variation)
  }

  private def getFullPremisesFee(variation: AmendVariationResponse): BigDecimal = {
    Premises.feePer * variation.addedFullYearTradingPremises
  }

  private def getHalfYearPremisesFee(variation: AmendVariationResponse): BigDecimal = {
    PremisesHalfYear.feePer * variation.halfYearlyTradingPremises
  }

  private def getPeopleFee(variation: AmendVariationResponse): BigDecimal =
    People.feePer * (variation.addedResponsiblePeople + variation.addedResponsiblePeopleFitAndProper)

  private def getFitAndProperDeduction(variation: AmendVariationResponse): BigDecimal =
    0 - (People.feePer * variation.addedResponsiblePeopleFitAndProper)

  private def getBreakdownRows
  (submission: SubmissionResponse,
   premises: Seq[TradingPremises],
   people: Seq[ResponsiblePeople],
   subQuantity: Int): Seq[BreakdownRow] = {
    Seq(
      BreakdownRow(Submission.message, subQuantity, Submission.feePer, subQuantity * Submission.feePer)
    ) ++ responsiblePeopleRows(people, submission) ++
      Seq(BreakdownRow(Premises.message, premises.size, Premises.feePer, submission.premiseFee))
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

  def bankDetailsExceptDeleted(bankDetails: Option[Seq[BankDetails]]): Option[Seq[BankDetails]] = {
    bankDetails match {
      case Some(bankAccts) => {
        val bankDtls = bankAccts.filterNot(x => x.status.contains(StatusConstants.Deleted) || x.bankAccountType.isEmpty)
        bankDtls.nonEmpty match {
          case true => Some(bankDtls)
          case false => Some(Seq.empty)
        }
      }
      case _ => Some(Seq.empty)
    }
  }

  private def subscriptionQuantity(subscription: SubmissionResponse): Int =
    if (subscription.registrationFee == 0) 0 else 1

  private def responsiblePeopleRows(people: Seq[ResponsiblePeople], subscription: SubmissionResponse): Seq[BreakdownRow] = {

    val max = (x: BigDecimal, y: BigDecimal) => if (x > y) x else y

    people.filter(!_.status.contains(StatusConstants.Deleted)).partition(_.hasAlreadyPassedFitAndProper.getOrElse(false)) match {
      case (b, a) =>
        Seq(BreakdownRow(People.message, a.size, People.feePer, Currency.fromBD(subscription.fPFee.getOrElse(0)))) ++
          (if (b.nonEmpty) {
            Seq(BreakdownRow(UnpaidPeople.message, b.size, max(0, UnpaidPeople.feePer), Currency.fromBD(max(0, UnpaidPeople.feePer))))
          } else {
            Seq.empty
          })
    }
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
