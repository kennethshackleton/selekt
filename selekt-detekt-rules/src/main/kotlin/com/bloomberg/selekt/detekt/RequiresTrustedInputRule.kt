/*
 * Copyright 2026 Bloomberg Finance L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bloomberg.selekt.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall

class RequiresTrustedInputRule(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        id = "RequiresTrustedInput",
        severity = Severity.Security,
        description = "Argument passed to a @RequiresTrustedInput-annotated parameter must be a compile-time " +
            "constant string literal or a value transitively derived from one — dynamic identifiers can lead to " +
            "SQL injection.",
        debt = Debt.TWENTY_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (bindingContext == BindingContext.EMPTY) return
        val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
        val calleeAnnotated = resolvedCall.resultingDescriptor.annotations.hasAnnotation(TRUSTED_INPUT_FQN)
        resolvedCall.valueArguments.forEach { (parameter, argument) ->
            val parameterAnnotated = parameter.annotations.hasAnnotation(TRUSTED_INPUT_FQN)
            if (!parameterAnnotated && !calleeAnnotated) return@forEach
            argument.arguments.forEach { valueArg ->
                val argExpression = valueArg.getArgumentExpression() ?: return@forEach
                if (!isTrusted(argExpression)) {
                    val parameterName = parameter.name.asString()
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(argExpression),
                            "Argument for @RequiresTrustedInput parameter '$parameterName' must be a compile-time " +
                                "constant string or a value marked @RequiresTrustedInput."
                        )
                    )
                }
            }
        }
    }

    private fun isTrusted(expression: KtExpression): Boolean {
        if (expression is KtStringTemplateExpression) {
            return expression.entries.all { it is KtLiteralStringTemplateEntry }
        }
        if (expression is KtConstantExpression) {
            return true
        }
        if (expression is KtCallExpression) {
            val callName = expression.calleeExpression?.text
            if (callName == "arrayOf" || callName == "listOf" || callName == "setOf") {
                return expression.valueArguments.all { arg ->
                    val inner = arg.getArgumentExpression() ?: return@all false
                    isTrusted(inner)
                }
            }
            val resolvedCall = expression.getResolvedCall(bindingContext) ?: return false
            return resolvedCall.resultingDescriptor.annotations.hasAnnotation(TRUSTED_INPUT_FQN)
        }
        if (expression is KtNameReferenceExpression) {
            val target = bindingContext[BindingContext.REFERENCE_TARGET, expression] ?: return false
            if (target is VariableDescriptor && target.isConst) {
                return true
            }
            if (target is CallableDescriptor && target.annotations.hasAnnotation(TRUSTED_INPUT_FQN)) {
                return true
            }
        }
        return false
    }

    companion object {
        private val TRUSTED_INPUT_FQN = FqName("com.bloomberg.selekt.annotations.RequiresTrustedInput")
    }
}
