package services

import config.ApplicationConfig
import connectors.{AmlsConnector, DataCacheConnector, GovernmentGatewayConnector}
import exceptions.NoEnrolmentException
import models.aboutthebusiness.AboutTheBusiness
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.{BusinessMatching, TrustAndCompanyServices, BusinessActivities => BusinessSevices, MoneyServiceBusiness => MSB}
import models.confirmation.{BreakdownRow, Currency}
import models.declaration.AddPerson
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.renewal.{Renewal, RenewalResponse}
import models.responsiblepeople.ResponsiblePeople
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import models.{AmendVariationResponse, SubmissionResponse, SubscriptionRequest, SubscriptionResponse}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.StatusConstants
import models.renewal.Conversions._

import scala.concurrent.{ExecutionContext, Future}

trait SubmissionService extends DataCacheService {

  private[services] def cacheConnector: DataCacheConnector
  private[services] def amlsConnector: AmlsConnector
  private[services] def ggService: GovernmentGatewayService
  private[services] def authEnrolmentsService: AuthEnrolmentsService

  private case class RowEntity(message: String, feePer: BigDecimal)

  private def submissionRowEntity(response :SubmissionResponse) = RowEntity("confirmation.submission", response.registrationFee)
  private def premisesRowEntity(response :SubmissionResponse) = RowEntity("confirmation.tradingpremises",
    response.premiseFeeRate.getOrElse(ApplicationConfig.premisesFee))
  private def premisesHalfYear(response :SubmissionResponse) = RowEntity("confirmation.tradingpremises.half",
    premisesRowEntity(response).feePer / 2)
  private val PremisesZero = RowEntity("confirmation.tradingpremises.zero", 0)
  private def peopleRowEntity(response :SubmissionResponse) = RowEntity("confirmation.responsiblepeople",
    response.fpFeeRate.getOrElse(ApplicationConfig.peopleFee))
  private val UnpaidPeople = RowEntity("confirmation.unpaidpeople", 0)

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
          businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
          businessActivities <- businessMatching.activities
        } yield {
          val subQuantity = subscriptionQuantity(subscription)
          val paymentReference = subscription.paymentReference
          val total = subscription.totalFees
          val rows = getBreakdownRows(subscription, premises, people, businessActivities, subQuantity)
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

  def renewal(renewal: Renewal)(implicit authContext: AuthContext, headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[SubmissionResponse] = {
    for {
      cache <- getCache
      regNo <- authEnrolmentsService.amlsRegistrationNumber
      response <- amlsConnector.renewal(
        createSubscriptionRequest(cache).withRenewalData(renewal),
        regNo.getOrElse(throw new NoEnrolmentException("[SubmissionService][renewal] - No enrolment"))
      )
      _ <- cacheConnector.save[RenewalResponse](RenewalResponse.key, response)
    } yield response
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
      businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      businessActivities <- businessMatching.activities
    } yield {
      val subQuantity = subscriptionQuantity(amendment)
      val total = amendment.totalFees
      val difference = amendment.difference map Currency.fromBD
      val filteredPremises = premises.filter(!_.status.contains(StatusConstants.Deleted))
      val rows = getBreakdownRows(amendment, filteredPremises, people, businessActivities, subQuantity)
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
          val totalFees: BigDecimal = peopleFee + premisesFee
          val rows = getVariationBreakdown(variation, peopleFee)
          val paymentRef = variation.paymentReference
          Future.successful(Some((paymentRef, Currency(totalFees), rows)))
        }) getOrElse Future.failed(new Exception("Cannot get amendment response"))
    }
  }

  def getRenewal
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[Option[(Option[String], Currency, Seq[BreakdownRow])]] = {
    cacheConnector.fetchAll flatMap {
      option =>
        (for {
          cache <- option
          renewal <- cache.getEntry[RenewalResponse](RenewalResponse.key)
        } yield {
          val premisesFee: BigDecimal = getRenewalTotalPremisesFee(renewal)
          val peopleFee: BigDecimal = getRenewalPeopleFee(renewal)
          val totalFees: BigDecimal = peopleFee + premisesFee
          val rows = getRenewalBreakdown(renewal, peopleFee)
          val paymentRef = renewal.paymentReference
          Future.successful(Some((paymentRef, Currency(totalFees), rows)))
        }) getOrElse Future.failed(new Exception("Cannot get amendment response"))
    }
  }

  private def getVariationBreakdown(variation: AmendVariationResponse, peopleFee: BigDecimal): Seq[BreakdownRow] = {

    val breakdownRows = Seq()

    def variationRow(count: Int, rowEntity: RowEntity, total: AmendVariationResponse => BigDecimal): Seq[BreakdownRow] = {
      if (count > 0) {
        breakdownRows ++ Seq(BreakdownRow(rowEntity.message, count, rowEntity.feePer, Currency(total(variation))))
      } else {
        Seq()
      }
    }

    def rpRow: Seq[BreakdownRow] = variationRow(variation.addedResponsiblePeople, peopleRowEntity(variation), getPeopleFee)
    def fpRow: Seq[BreakdownRow] = variationRow(variation.addedResponsiblePeopleFitAndProper, UnpaidPeople, getFitAndProperDeduction)

    def tpFullYearRow: Seq[BreakdownRow] = variationRow(variation.addedFullYearTradingPremises, premisesRowEntity(variation), getFullPremisesFee)
    def tpHalfYearRow: Seq[BreakdownRow] = variationRow(variation.halfYearlyTradingPremises, premisesHalfYear(variation), getHalfYearPremisesFee)
    def tpZeroRow: Seq[BreakdownRow] = variationRow(variation.zeroRatedTradingPremises, PremisesZero, getZeroPremisesFee)

    rpRow ++ fpRow ++ tpZeroRow ++ tpHalfYearRow ++ tpFullYearRow

  }

  private def getRenewalBreakdown(renewal: RenewalResponse, peopleFee: BigDecimal): Seq[BreakdownRow] = {

    val breakdownRows = Seq()

    def renewalRow(count: Int, rowEntity: RowEntity, total: RenewalResponse => BigDecimal): Seq[BreakdownRow] = {
      if (count > 0) {
        breakdownRows ++ Seq(BreakdownRow(rowEntity.message, count, rowEntity.feePer, Currency(total(renewal))))
      } else {
        Seq()
      }
    }

    def rpRow: Seq[BreakdownRow] = renewalRow(renewal.addedResponsiblePeople, peopleRowEntity(renewal), getRenewalPeopleFee)
    def fpRow: Seq[BreakdownRow] = renewalRow(renewal.addedResponsiblePeopleFitAndProper, UnpaidPeople, getRenewalFitAndProperDeduction)

    def tpFullYearRow: Seq[BreakdownRow] = renewalRow(renewal.addedFullYearTradingPremises, premisesRowEntity(renewal), getRenewalFullPremisesFee)
    def tpHalfYearRow: Seq[BreakdownRow] = renewalRow(renewal.halfYearlyTradingPremises, premisesHalfYear(renewal), getRenewalHalfYearPremisesFee)
    def tpZeroRow: Seq[BreakdownRow] = renewalRow(renewal.zeroRatedTradingPremises, PremisesZero, getRenewalZeroPremisesFee)

    rpRow ++ fpRow ++ tpZeroRow ++ tpHalfYearRow ++ tpFullYearRow

  }

  private def getRenewalTotalPremisesFee(renewal: RenewalResponse): BigDecimal =
    (premisesRowEntity(renewal).feePer * renewal.addedFullYearTradingPremises) + getRenewalHalfYearPremisesFee(renewal)

  private def getRenewalFullPremisesFee(renewal: RenewalResponse): BigDecimal =
    premisesRowEntity(renewal).feePer * renewal.addedFullYearTradingPremises

  private def getRenewalHalfYearPremisesFee(renewal: RenewalResponse): BigDecimal =
    premisesHalfYear(renewal).feePer * renewal.halfYearlyTradingPremises

  private def getRenewalPeopleFee(renewal: RenewalResponse): BigDecimal =
    peopleRowEntity(renewal).feePer * renewal.addedResponsiblePeople

  private def getRenewalFitAndProperDeduction(renewal: RenewalResponse): BigDecimal = 0

  private def getRenewalZeroPremisesFee(renewal: RenewalResponse): BigDecimal = 0

  private def getTotalPremisesFee(variation: AmendVariationResponse): BigDecimal =
    (premisesRowEntity(variation).feePer * variation.addedFullYearTradingPremises) + getHalfYearPremisesFee(variation)

  private def getFullPremisesFee(variation: AmendVariationResponse): BigDecimal =
    premisesRowEntity(variation).feePer * variation.addedFullYearTradingPremises

  private def getHalfYearPremisesFee(variation: AmendVariationResponse): BigDecimal =
    premisesHalfYear(variation).feePer * variation.halfYearlyTradingPremises

  private def getZeroPremisesFee(variation: AmendVariationResponse): BigDecimal = 0

  private def getPeopleFee(variation: AmendVariationResponse): BigDecimal =
    peopleRowEntity(variation).feePer * variation.addedResponsiblePeople

  private def getFitAndProperDeduction(variation: AmendVariationResponse): BigDecimal = 0

  private def getBreakdownRows
  (submission: SubmissionResponse,
   premises: Seq[TradingPremises],
   people: Seq[ResponsiblePeople],
   businessActivities: BusinessSevices,
   subQuantity: Int): Seq[BreakdownRow] = {
    Seq(BreakdownRow(submissionRowEntity(submission).message, subQuantity,
      submissionRowEntity(submission).feePer, subQuantity * submissionRowEntity(submission).feePer)) ++
      responsiblePeopleRows(people, submission, businessActivities) ++
      Seq(BreakdownRow(premisesRowEntity(submission).message, premises.size, premisesRowEntity(submission).feePer, submission.premiseFee))
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

  private def responsiblePeopleRows(
                                     people: Seq[ResponsiblePeople],
                                     subscription: SubmissionResponse,
                                     businessActivities: BusinessSevices
                                   ): Seq[BreakdownRow] = {

    val showBreakdown = subscription.fpFee match {
      case None => businessActivities.businessActivities.exists(act => act == MSB || act == TrustAndCompanyServices)
      case _ => true
    }

    if(showBreakdown){

      val max = (x: BigDecimal, y: BigDecimal) => if (x > y) x else y

      people.filter(!_.status.contains(StatusConstants.Deleted)).partition(_.hasAlreadyPassedFitAndProper.getOrElse(false)) match {
        case (b, a) =>
          Seq(BreakdownRow(peopleRowEntity(subscription).message, a.size, peopleRowEntity(subscription).feePer,
            Currency.fromBD(subscription.fpFee.getOrElse(0)))) ++
            (if (b.nonEmpty) {
              Seq(BreakdownRow(UnpaidPeople.message, b.size, max(0, UnpaidPeople.feePer), Currency.fromBD(max(0, UnpaidPeople.feePer))))
            } else {
              Seq.empty
            })
      }
    } else {
      Seq.empty
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
