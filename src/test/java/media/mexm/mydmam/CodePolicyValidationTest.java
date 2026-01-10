/*
 * This file is part of mydmam.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2025
 *
 */
package media.mexm.mydmam;

class CodePolicyValidationTest /*extends CheckPolicy*/ { // NOSONAR S2187

	/**
	 * CodePolicyValidation don't works...
	 * --
	 * Cryptic error:
	 * --
	 * spoon.JLSViolation: Not allowed javaletter or keyword in identifier found. See JLS for correct identifier. Identifier: T'
	 * at spoon.JLSViolation.throwIfSyntaxErrorsAreNotIgnored(JLSViolation.java:38)
	 * at spoon.support.reflect.reference.CtReferenceImpl.checkIdentifierForJLSCorrectness(CtReferenceImpl.java:114)
	 * at spoon.support.reflect.reference.CtReferenceImpl.setSimpleName(CtReferenceImpl.java:57)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getTypeReferenceFromTypeVariableBinding(ReferenceBuilder.java:1026)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getTypeReference(ReferenceBuilder.java:863)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getTypeReference(ReferenceBuilder.java:842)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getTypeReferenceFromTypeArgument(ReferenceBuilder.java:976)
	 * at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:214)
	 * at java.base/java.util.Spliterators$ArraySpliterator.forEachRemaining(Spliterators.java:1024)
	 * at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:570)
	 * at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:560)
	 * at java.base/java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:921)
	 * at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:265)
	 * at java.base/java.util.stream.ReferencePipeline.collect(ReferencePipeline.java:723)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getTypeArguments(ReferenceBuilder.java:961)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getParameterizedTypeReference(ReferenceBuilder.java:948)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getTypeReference(ReferenceBuilder.java:857)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getTypeReference(ReferenceBuilder.java:842)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getTypeReferenceFromTypeArgument(ReferenceBuilder.java:976)
	 * at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:214)
	 * at java.base/java.util.Spliterators$ArraySpliterator.forEachRemaining(Spliterators.java:1024)
	 * at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:570)
	 * at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:560)
	 * at java.base/java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:921)
	 * at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:265)
	 * at java.base/java.util.stream.ReferencePipeline.collect(ReferencePipeline.java:723)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getTypeArguments(ReferenceBuilder.java:961)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getParameterizedTypeReference(ReferenceBuilder.java:948)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getTypeReference(ReferenceBuilder.java:857)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getExecutableReference(ReferenceBuilder.java:475)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getExecutableReference(ReferenceBuilder.java:414)
	 * at spoon.support.compiler.jdt.ReferenceBuilder.getExecutableReference(ReferenceBuilder.java:493)
	 * at spoon.support.compiler.jdt.JDTTreeBuilder.visit(JDTTreeBuilder.java:893)
	 * at org.eclipse.jdt.internal.compiler.ast.AllocationExpression.traverse(AllocationExpression.java:730)
	 * at org.eclipse.jdt.internal.compiler.ast.Assignment.traverse(Assignment.java:287)
	 * at org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration.traverse(ConstructorDeclaration.java:712)
	 * at org.eclipse.jdt.internal.compiler.ast.TypeDeclaration.traverse(TypeDeclaration.java:1716)
	 * at org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration.traverse(CompilationUnitDeclaration.java:829)
	 * at org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration.traverse(CompilationUnitDeclaration.java:790)
	 * at spoon.support.compiler.jdt.JDTBasedSpoonCompiler.traverseUnitDeclaration(JDTBasedSpoonCompiler.java:504)
	 * at spoon.support.compiler.jdt.JDTBasedSpoonCompiler.lambda$buildModel$0(JDTBasedSpoonCompiler.java:461)
	 * at spoon.support.compiler.jdt.JDTBasedSpoonCompiler.forEachCompilationUnit(JDTBasedSpoonCompiler.java:488)
	 * at spoon.support.compiler.jdt.JDTBasedSpoonCompiler.buildModel(JDTBasedSpoonCompiler.java:459)
	 * at spoon.support.compiler.jdt.JDTBasedSpoonCompiler.buildUnitsAndModel(JDTBasedSpoonCompiler.java:388)
	 * at spoon.support.compiler.jdt.JDTBasedSpoonCompiler.buildSources(JDTBasedSpoonCompiler.java:346)
	 * at spoon.support.compiler.jdt.JDTBasedSpoonCompiler.build(JDTBasedSpoonCompiler.java:117)
	 * at spoon.support.compiler.jdt.JDTBasedSpoonCompiler.build(JDTBasedSpoonCompiler.java:100)
	 * at spoon.Launcher.buildModel(Launcher.java:781)
	 * at tv.hd3g.commons.codepolicyvalidation.Policies.globalInit(Policies.java:61)
	 * at tv.hd3g.commons.codepolicyvalidation.CheckPolicy.globalInit(CheckPolicy.java:92)
	 * at java.base/java.lang.reflect.Method.invoke(Method.java:565)
	 * at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
	 */
}
