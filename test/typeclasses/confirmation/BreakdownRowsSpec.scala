/*
 * Copyright 2018 HM Revenue & Customs
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

package typeclasses.confirmation

import org.scalatestplus.play.PlaySpec

// TODO: Implement
class BreakdownRowsSpec extends PlaySpec {

    "value is a AmendVariationRenewalResponse" when {
        "businessActivities contains MSB" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }

        "businessActivities contains TCSP" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }

        "businessActivities contains neither MSB or TCSP" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }
    }

    "value is a SubmissionResponse" when {
        "businessActivities contains MSB" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }

        "businessActivities contains TCSP" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }

        "businessActivities contains neither MSB or TCSP" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }
    }

}
