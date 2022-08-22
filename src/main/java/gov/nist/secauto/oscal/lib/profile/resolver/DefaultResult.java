/*
 * Portions of this software was developed by employees of the National Institute
 * of Standards and Technology (NIST), an agency of the Federal Government and is
 * being made available as a public service. Pursuant to title 17 United States
 * Code Section 105, works of NIST employees are not subject to copyright
 * protection in the United States. This software may be subject to foreign
 * copyright. Permission in the United States and in foreign countries, to the
 * extent that NIST may hold copyright, to use, copy, modify, create derivative
 * works, and distribute this software and its documentation without fee is hereby
 * granted on a non-exclusive basis, provided that this notice and disclaimer
 * of warranty appears in all copies.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, EITHER
 * EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY
 * THAT THE SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND FREEDOM FROM
 * INFRINGEMENT, AND ANY WARRANTY THAT THE DOCUMENTATION WILL CONFORM TO THE
 * SOFTWARE, OR ANY WARRANTY THAT THE SOFTWARE WILL BE ERROR FREE.  IN NO EVENT
 * SHALL NIST BE LIABLE FOR ANY DAMAGES, INCLUDING, BUT NOT LIMITED TO, DIRECT,
 * INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES, ARISING OUT OF, RESULTING FROM,
 * OR IN ANY WAY CONNECTED WITH THIS SOFTWARE, WHETHER OR NOT BASED UPON WARRANTY,
 * CONTRACT, TORT, OR OTHERWISE, WHETHER OR NOT INJURY WAS SUSTAINED BY PERSONS OR
 * PROPERTY OR OTHERWISE, AND WHETHER OR NOT LOSS WAS SUSTAINED FROM, OR AROSE OUT
 * OF THE RESULTS OF, OR USE OF, THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.
 */

package gov.nist.secauto.oscal.lib.profile.resolver;

import gov.nist.secauto.metaschema.model.common.util.CollectionUtil;
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.CatalogGroup;
import gov.nist.secauto.oscal.lib.model.Control;
import gov.nist.secauto.oscal.lib.model.Parameter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class DefaultResult implements IResult {
  private static final Logger LOGGER = LogManager.getLogger(DefaultResult.class);

  @NonNull
  private final Set<Parameter> promotedParameters;
  @NonNull
  private final Set<Control> promotedControls;
  @NonNull
  private final Set<String> requiredParameterIds;

  public DefaultResult() {
    this.promotedParameters = new LinkedHashSet<>();
    this.promotedControls = new LinkedHashSet<>();
    this.requiredParameterIds = new HashSet<>();
  }

  @Override
  @NonNull
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "intending to expose this field")
  public Collection<Parameter> getPromotedParameters() {
    return promotedParameters;
  }

  @Override
  @NonNull
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "intending to expose this field")
  public Collection<Control> getPromotedControls() {
    return promotedControls;
  }

  @Override
  @NonNull
  public Set<String> getRequiredParameterIds() {
    return CollectionUtil.unmodifiableSet(requiredParameterIds);
  }

  @Override
  public void requireParameters(@NonNull Set<String> requiredParameterIds) {
    this.requiredParameterIds.addAll(requiredParameterIds);
  }

  @Override
  public boolean isParameterRequired(@NonNull String id) {
    return getRequiredParameterIds().contains(id);
  }

  @Override
  public void promoteParameter(@NonNull Parameter param) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.atDebug().log("promoting parameter '{}'", param.getId());
    }
    promotedParameters.add(param);
  }

  @Override
  public void promoteControl(@NonNull Control control) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.atDebug().log("promoting control '{}'", control.getId());
    }
    promotedControls.add(control);
  }

  @Override
  public void applyTo(@NonNull Catalog parent) {
    getPromotedParameters().forEach(param -> parent.addParam(param));
    getPromotedControls().forEach(control -> {
      parent.addControl(control);
      control.setParentControl(null);
    });
  }

  @Override
  public void applyTo(@NonNull CatalogGroup parent) {
    getPromotedParameters().forEach(param -> parent.addParam(param));
    getPromotedControls().forEach(control -> {
      parent.addControl(control);
      control.setParentControl(null);
    });
  }

  @Override
  public void applyTo(@NonNull Control parent) {
    getPromotedParameters().forEach(param -> parent.addParam(param));
    getPromotedControls().forEach(control -> {
      parent.addControl(control);
      control.setParentControl(parent);
    });
  }

  @Override
  public IResult append(IResult that) {
    promotedParameters.addAll(that.getPromotedParameters());
    promotedControls.addAll(that.getPromotedControls());
    requiredParameterIds.addAll(that.getRequiredParameterIds());
    return this;
  }
}