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

package gov.nist.secauto.oscal.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.umd.cs.findbugs.annotations.NonNull;
import gov.nist.secauto.metaschema.binding.io.Format;
import gov.nist.secauto.metaschema.binding.io.IBoundLoader;
import gov.nist.secauto.metaschema.binding.io.ISerializer;
import gov.nist.secauto.metaschema.model.common.datatype.markup.MarkupLine;
import gov.nist.secauto.metaschema.model.common.datatype.markup.MarkupMultiline;
import gov.nist.secauto.metaschema.model.common.util.ObjectUtils;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.model.Address;
import gov.nist.secauto.oscal.lib.model.BackMatter;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource;
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.CatalogGroup;
import gov.nist.secauto.oscal.lib.model.Control;
import gov.nist.secauto.oscal.lib.model.ControlPart;
import gov.nist.secauto.oscal.lib.model.Link;
import gov.nist.secauto.oscal.lib.model.Metadata;
import gov.nist.secauto.oscal.lib.model.Metadata.Party;
import gov.nist.secauto.oscal.lib.model.Metadata.Role;
import gov.nist.secauto.oscal.lib.model.Profile;
import gov.nist.secauto.oscal.lib.model.Property;
import gov.nist.secauto.oscal.lib.model.ResponsibleParty;

public class CodeBuilderInstanceTest {
  private static final URI OSCAL_DEFAULT_NS = URI.create("http://csrc.nist.gov/ns/oscal");
  private static OscalBindingContext bindingContext;

  @SuppressWarnings("null")
  @NonNull
  static Path newPath(@NonNull Path dir, @NonNull String filename) {
    return dir.resolve(filename);
  }

  @BeforeAll
  static void initialize() { // NOPMD actually used
    bindingContext = OscalBindingContext.instance();
  }

  @Test
  void testBuildInstanceWithCodeNoFile(@TempDir Path tempDir) throws IOException {
    // Initialize timestamps for use in metadata
    ZonedDateTime now = ZonedDateTime.now();
    // Build role, party, and responsible party before you add it to the
    // metadata.
    Address address = new Address();
    address.setAddrLines(
        new ArrayList(Arrays.asList(new String[] { "100 Main Street", "Floor 2", "Office 201" })));
    address.setCity("Creator City");
    address.setState("Creator State");
    address.setCountry("US");
    address.setPostalCode("12345-6789");
    address.setType("work");
    UUID creatorPartyUuid = UUID.randomUUID();
    Party creatorParty = new Party();
    creatorParty.setUuid(creatorPartyUuid);
    creatorParty.setType("organization");
    creatorParty.setName("Example Catalog Creator Party");
    creatorParty.addAddress(address);
    creatorParty.addEmailAddress("creator@example.org");
    // Build responsible-party binding creator to organization
    String[] roleIds = new String[] { "author", "creator", "contact" };
    ArrayList<Role> roles = new ArrayList<Role>();
    ArrayList<ResponsibleParty> responsibleParties = new ArrayList<ResponsibleParty>();
    for (String roleId : roleIds) {
      Role role = new Role();
      role.setId(roleId);
      role.setTitle(MarkupLine.fromMarkdown(roleId + " role for this catalog"));
      roles.add(role);
      ResponsibleParty responsibleParty = new ResponsibleParty();
      responsibleParty.setRoleId(roleId);
      // Use the same creator UUID for the responsible party for all roles
      // in this catalog.
      responsibleParty.addPartyUuid(creatorPartyUuid);
      responsibleParties.add(responsibleParty);
    }
    // Build back-matter resources first to reference them by UUID in the
    // metadata later.
    BackMatter backmatter = new BackMatter();
    UUID resourceUuid = UUID.randomUUID();
    Resource pdfCatalog = new Resource();
    Resource.Rlink pdfCatalogLink = new Resource.Rlink();
    pdfCatalog.setUuid(resourceUuid);
    pdfCatalog.setTitle(MarkupLine.fromMarkdown("Canonical PDF version of catalog"));
    pdfCatalogLink.setMediaType("application/pdf");
    pdfCatalogLink.setHref(URI.create("https://example.org/catalog.pdf"));
    pdfCatalog.addRlink(pdfCatalogLink);
    backmatter.addResource(pdfCatalog);
    Metadata metadata = new Metadata();
    MarkupLine catalogTitle = MarkupLine.fromMarkdown("Test Code-Generated Catalog");
    Link link = new Link();
    link.setRel("canonical");
    link.setHref(URI.create("#" + resourceUuid.toString()));
    // Build keyword before you add it to the metadata
    Property keywordsProp = Property.builder("keywords").namespace(OSCAL_DEFAULT_NS).value(
        "critical infrastructure, cybersecurity, information security, information system, OSCAL, Open Security Controls Assessment Language, security functions, security requirements, system, system security")
        .build();
    metadata.setTitle(catalogTitle);
    metadata.setVersion("0.1.0-alpha");
    metadata.setOscalVersion("1.1.1");
    metadata.setLastModified(now);
    metadata.setPublished(now);
    metadata.addProp(keywordsProp);
    metadata.addLink(link);
    metadata.addParty(creatorParty);
    // You do not need to add one party at a time, this is an example of using
    // set.... function template to add a list you have pre-populated, because
    // unlike other metadata elements there is more than one.
    metadata.setRoles(roles);
    metadata.setResponsibleParties(responsibleParties);
    // Build group with controls and parts before adding it catalog
    CatalogGroup group = new CatalogGroup();
    group.setClazz("function");
    group.setId("G1");
    group.setTitle(MarkupLine.fromMarkdown("Group 1"));
    ControlPart overviewPart = new ControlPart();
    overviewPart.setNs(OSCAL_DEFAULT_NS);
    overviewPart.setName("overview");
    overviewPart.setId("G1_overview");
    group.addPart(overviewPart);
    Control control = new Control();
    control.setClazz("category");
    control.setId("G1.C1");
    control.setTitle(MarkupLine.fromMarkdown("Title of G1.C1"));
    ControlPart controlStatementPart = new ControlPart();
    controlStatementPart.setName("statement");
    controlStatementPart.setNs(OSCAL_DEFAULT_NS);
    controlStatementPart.setId("G1.C1_statement");
    // You do not have to only use Markdown for Markup or MarkupMultiline, you
    // can also use HTML inline to control the structure.
    controlStatementPart.setProse(MarkupMultiline.fromHtml(
        "<p>This is the actual text of the control for Control 1 in Group 1 of this catalog.</p><p>This HTML example shows how it can have multiple lines.</p>"));
    control.addPart(controlStatementPart);
    Control subControl = new Control();
    subControl.setClazz("category");
    subControl.setId("G1.C1.SC1");
    subControl.setTitle(MarkupLine.fromMarkdown("Title of G1.C1.SC1"));
    ControlPart subControlStatementPart = new ControlPart();
    subControlStatementPart.setNs(OSCAL_DEFAULT_NS);
    subControlStatementPart.setName("statement");
    subControlStatementPart.setId("G1.C1.SC1_statement");
    // You do not have to only use Markdown for Markup or MarkupMultiline, you
    // can also use HTML inline to control the structure.
    subControlStatementPart.setProse(MarkupMultiline.fromMarkdown(
        "This is the actual text of the control for Subcontrol 1 within Control 1 in Group 1 of this catalog.\n" + "\n"
            + "This Markdown example shows how it can have multiple lines."));
    subControl.addPart(subControlStatementPart);
    control.addControl(subControl);
    group.addControl(control);
    Catalog catalog = new Catalog();
    // Add pre-populated back-matter to catalog
    catalog.setBackMatter(backmatter);
    // Add pre-populated metadata to catalog
    catalog.setMetadata(metadata);
    // Add group with part and nested controls
    catalog.addGroup(group);
    assertEquals(catalog.getMetadata().getTitle().toMarkdown().toString(), "Test Code-Generated Catalog");
    assertEquals(catalog.getMetadata().getVersion(), "0.1.0-alpha");
    assertEquals(catalog.getMetadata().getLinks().size(), 1);
    assertEquals(catalog.getMetadata().getParties().size(), 1);
    // This is now always the case but there is 1 party and 3 roles, and the
    // party is bound by UUID to all 3 roles in the responsible-parties element,
    // so their count should be equal in this catalog.
    assertEquals(catalog.getMetadata().getRoles().size(), catalog.getMetadata().getResponsibleParties().size());
    // Check group
    assertEquals(catalog.getGroups().size(), 1);
    // Check number of parts at the group level, should be 1
    assertEquals(catalog.getGroups().get(0).getParts().size(), 1);
    // Check number of controls, without sub-controls, should be 1, it doesn't
    // search recursively by default
    assertEquals(catalog.getGroups().get(0).getControls().size(), 1);
    // Check number of sub-controls in that one control, should be 1, it does
    // not include the parent control
    assertEquals(catalog.getGroups().get(0).getControls().get(0).getControls().size(), 1);

    // For convenience, let's serialize it out in the three formats for devs to
    // inspect and review
    for (Format format : Format.values()) {
      Path out = newPath(ObjectUtils.notNull(tempDir), "code-generated_catalog" +
          format.getDefaultExtension());
      ISerializer<Catalog> serializer = bindingContext.newSerializer(format, Catalog.class);
      serializer.serialize(catalog, out);
      assertNotNull(bindingContext.loadCatalog(out));
    }
  }
}
