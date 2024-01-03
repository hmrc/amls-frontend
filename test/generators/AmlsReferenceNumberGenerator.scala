/*
 * Copyright 2024 HM Revenue & Customs
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

package generators

import org.scalacheck.Gen

//noinspection ScalaStyle
trait AmlsReferenceNumberGenerator {

  def amlsRefNoGen = {
    for {
      a <- Gen.listOfN(1, Gen.alphaUpperChar).map(x => x.mkString)
      b <- Gen.listOfN(6, Gen.numChar).map(x => x.mkString)
    } yield s"X${a}ML00000$b"
  }

  lazy val amlsRegistrationNumber = amlsRefNoGen.sample.get
}
