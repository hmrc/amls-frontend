/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package generators.enrolment

import generators.BaseGenerator
import models.enrolment.{ESEnrolment, EnrolmentEntry, EnrolmentIdentifier}
import org.scalacheck.Gen

//noinspection ScalaStyle
trait ESEnrolmentGenerator extends BaseGenerator {
  val identifierGen: Gen[EnrolmentIdentifier] = for {
    key <- stringOfLengthGen(10)
    value <- stringOfLengthGen(10)
  } yield EnrolmentIdentifier(key, value)

  val enrolmentEntriesGen: Gen[EnrolmentEntry] = for {
    serviceName <- stringOfLengthGen(6)
    numIdentifiers <- Gen.choose(1, 5)
    identifiers <- Gen.listOfN(numIdentifiers, identifierGen)
  } yield EnrolmentEntry(serviceName, "active", "Generated enrolment", identifiers)

  val esEnrolmentGen: Gen[ESEnrolment] = for {
    numEntries <- Gen.choose(1, 5)
    entries <- Gen.listOfN(numEntries, enrolmentEntriesGen)
  } yield ESEnrolment(1, numEntries, entries)

  def esEnrolmentWith(entry: EnrolmentEntry) = for {
    enrolment <- esEnrolmentGen
  } yield enrolment.copy(enrolments = enrolment.enrolments :+ entry)
}
