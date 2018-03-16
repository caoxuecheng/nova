package com.onescorpin.integration.nflow;

/*-
 * #%L
 * nova-nflow-manager-controller
 * %%
 * Copyright (C) 2017 Onescorpin
 * %%
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
 * #L%
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.CharMatcher;
import com.onescorpin.discovery.model.DefaultHiveSchema;
import com.onescorpin.discovery.model.DefaultTableSchema;
import com.onescorpin.discovery.model.DefaultTag;
import com.onescorpin.discovery.schema.Field;
import com.onescorpin.discovery.schema.Tag;
import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.NflowSchedule;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplate;
import com.onescorpin.nflowmgr.rest.model.schema.NflowProcessingOptions;
import com.onescorpin.nflowmgr.rest.model.schema.PartitionField;
import com.onescorpin.nflowmgr.rest.model.schema.TableOptions;
import com.onescorpin.nflowmgr.rest.model.schema.TableSetup;
import com.onescorpin.nflowmgr.service.nflow.ExportImportNflowService;
import com.onescorpin.nflowmgr.service.template.ExportImportTemplateService;
import com.onescorpin.integration.IntegrationTestBase;
import com.onescorpin.jobrepo.query.model.DefaultExecutedJob;
import com.onescorpin.jobrepo.query.model.DefaultExecutedStep;
import com.onescorpin.jobrepo.query.model.ExecutedStep;
import com.onescorpin.jobrepo.query.model.ExecutionStatus;
import com.onescorpin.jobrepo.query.model.ExitStatus;
import com.onescorpin.nifi.rest.model.NifiProperty;
import com.onescorpin.policy.rest.model.FieldPolicy;
import com.onescorpin.policy.rest.model.FieldStandardizationRule;
import com.onescorpin.policy.rest.model.FieldValidationRule;
import com.onescorpin.security.rest.model.User;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Basic Nflow Integration Test which imports data index nflow, creates category, imports data ingest template,
 * creates data ingest nflow, runs the nflow, validates number of executed jobs, validates validators and
 * standardisers have been applied by looking at profiler summary, validates total number and number of
 * valid and invalid rows, validates expected hive tables have been created and runs a simple hive
 * query and asserts the number of rows returned.
 *
 */
public class NflowIT extends IntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(NflowIT.class);

    private static final String SAMPLES_DIR = "/samples";
    private static final String DATA_SAMPLES_DIR = SAMPLES_DIR + "/sample-data/csv/";
    private static final String NIFI_NFLOW_SAMPLE_VERSION = "nifi-1.3";
    private static final String NIFI_TEMPLATE_SAMPLE_VERSION = "nifi-1.0";
    private static final String TEMPLATE_SAMPLES_DIR = SAMPLES_DIR + "/templates/" + NIFI_TEMPLATE_SAMPLE_VERSION + "/";
    private static final String NFLOW_SAMPLES_DIR = SAMPLES_DIR + "/nflows/" + NIFI_NFLOW_SAMPLE_VERSION + "/";
    protected static final String DATA_INGEST_ZIP = "data_ingest.zip";
    private static final String VAR_DROPZONE = "/var/dropzone";
    private static final String USERDATA1_CSV = "userdata1.csv";
    private static final int NFLOW_COMPLETION_WAIT_DELAY = 180;
    private static final int VALID_RESULTS = 879;
    private static final String INDEX_TEXT_SERVICE_V2_NFLOW_ZIP = "index_text_service_v2.nflow.zip";
    private static String NFLOW_NAME = "users_" + System.currentTimeMillis();
    private static String CATEGORY_NAME = "Functional Tests";

    private String sampleNflowsPath;
    protected String sampleTemplatesPath;
    private String usersDataPath;

    private FieldStandardizationRule toUpperCase = new FieldStandardizationRule();
    private FieldValidationRule email = new FieldValidationRule();
    private FieldValidationRule lookup = new FieldValidationRule();
    private FieldValidationRule notNull = new FieldValidationRule();
    private FieldStandardizationRule base64EncodeBinary = new FieldStandardizationRule();
    private FieldStandardizationRule base64EncodeString = new FieldStandardizationRule();
    private FieldStandardizationRule base64DecodeBinary = new FieldStandardizationRule();
    private FieldStandardizationRule base64DecodeString = new FieldStandardizationRule();
    private FieldValidationRule length = new FieldValidationRule();
    private FieldValidationRule ipAddress = new FieldValidationRule();

    @Override
    protected void configureObjectMapper(ObjectMapper om) {
        SimpleModule m = new SimpleModule();
        m.addAbstractTypeMapping(ExecutedStep.class, DefaultExecutedStep.class);
        om.registerModule(m);
    }

    @Test
    public void testDataIngestNflow() throws Exception {
        prepare();

        importSystemNflows();

        copyDataToDropzone();

        //create new category
        NflowCategory category = createCategory(CATEGORY_NAME);

        ExportImportTemplateService.ImportTemplate ingest = importDataIngestTemplate();

        //create standard ingest nflow
        NflowMetadata nflow = getCreateNflowRequest(category, ingest, NFLOW_NAME);
        NflowMetadata response = createNflow(nflow).getNflowMetadata();
        Assert.assertEquals(nflow.getNflowName(), response.getNflowName());

        waitForNflowToComplete();

        assertExecutedJobs(response);

        failJobs(response.getCategoryAndNflowName());
        abandonAllJobs(response.getCategoryAndNflowName());
    }

    @Test
    public void testEditNflow() throws Exception {
        // Prepare environment
        prepare();

        final NflowCategory category = createCategory(CATEGORY_NAME);
        final ExportImportTemplateService.ImportTemplate template = importDataIngestTemplate();

        // Create nflow
        NflowMetadata nflow = getCreateNflowRequest(category, template, NFLOW_NAME);
        nflow.setDescription("Test nflow");

        NflowMetadata response = createNflow(nflow).getNflowMetadata();
        Assert.assertEquals(nflow.getNflowName(), response.getNflowName());

        // Edit nflow
        nflow = response;
        nflow.setDescription(null);

        response = createNflow(nflow).getNflowMetadata();
        Assert.assertEquals(nflow.getNflowName(), response.getNflowName());
        Assert.assertEquals(nflow.getDescription(), response.getDescription());
    }

    @Override
    public void startClean() {
        super.startClean();
    }

//    @Test
    public void temp() {
//        failJobs("system.index_text_service");
    }

    protected void prepare() throws Exception {
        String path = getClass().getResource(".").toURI().getPath();
        String basedir = path.substring(0, path.indexOf("services"));
        sampleNflowsPath = basedir + NFLOW_SAMPLES_DIR;
        sampleTemplatesPath = basedir + TEMPLATE_SAMPLES_DIR;
        usersDataPath = basedir + DATA_SAMPLES_DIR;

        toUpperCase.setName("Uppercase");
        toUpperCase.setDisplayName("Uppercase");
        toUpperCase.setDescription("Convert string to uppercase");
        toUpperCase.setObjectClassType("com.onescorpin.policy.standardization.UppercaseStandardizer");
        toUpperCase.setObjectShortClassType("UppercaseStandardizer");

        email.setName("email");
        email.setDisplayName("Email");
        email.setDescription("Valid email address");
        email.setObjectClassType("com.onescorpin.policy.validation.EmailValidator");
        email.setObjectShortClassType("EmailValidator");

        ipAddress.setName("IP Address");
        ipAddress.setDisplayName("IP Address");
        ipAddress.setDescription("Valid IP address");
        ipAddress.setObjectClassType("com.onescorpin.policy.validation.IPAddressValidator");
        ipAddress.setObjectShortClassType("IPAddressValidator");

        lookup.setName("lookup");
        lookup.setDisplayName("Lookup");
        lookup.setDescription("Must be contained in the list");
        lookup.setObjectClassType("com.onescorpin.policy.validation.LookupValidator");
        lookup.setObjectShortClassType("LookupValidator");
        lookup.setProperties(newFieldRuleProperties(newFieldRuleProperty("List", "lookupList", "Male,Female")));

        base64DecodeBinary.setName("Base64 Decode");
        base64DecodeBinary.setDisplayName("Base64 Decode");
        base64DecodeBinary.setDescription("Base64 decode a string or a byte[].  Strings are evaluated using the UTF-8 charset");
        base64DecodeBinary.setObjectClassType("com.onescorpin.policy.standardization.Base64Decode");
        base64DecodeBinary.setObjectShortClassType("Base64Decode");
        base64DecodeBinary.setProperties(newFieldRuleProperties(newFieldRuleProperty("Output", "base64Output", "BINARY")));

        base64DecodeString.setName("Base64 Decode");
        base64DecodeString.setDisplayName("Base64 Decode");
        base64DecodeString.setDescription("Base64 decode a string or a byte[].  Strings are evaluated using the UTF-8 charset");
        base64DecodeString.setObjectClassType("com.onescorpin.policy.standardization.Base64Decode");
        base64DecodeString.setObjectShortClassType("Base64Decode");
        base64DecodeString.setProperties(newFieldRuleProperties(newFieldRuleProperty("Output", "base64Output", "STRING")));

        base64EncodeBinary.setName("Base64 Encode");
        base64EncodeBinary.setDisplayName("Base64 Encode");
        base64EncodeBinary.setDescription("Base64 encode a string or a byte[].  Strings are evaluated using the UTF-8 charset.  String output is urlsafe");
        base64EncodeBinary.setObjectClassType("com.onescorpin.policy.standardization.Base64Encode");
        base64EncodeBinary.setObjectShortClassType("Base64Encode");
        base64EncodeBinary.setProperties(newFieldRuleProperties(newFieldRuleProperty("Output", "base64Output", "BINARY")));

        base64EncodeString.setName("Base64 Encode");
        base64EncodeString.setDisplayName("Base64 Encode");
        base64EncodeString.setDescription("Base64 encode a string or a byte[].  Strings are evaluated using the UTF-8 charset.  String output is urlsafe");
        base64EncodeString.setObjectClassType("com.onescorpin.policy.standardization.Base64Encode");
        base64EncodeString.setObjectShortClassType("Base64Encode");
        base64EncodeString.setProperties(newFieldRuleProperties(newFieldRuleProperty("Output", "base64Output", "STRING")));

        notNull.setName("Not Null");
        notNull.setDisplayName("Not Null");
        notNull.setDescription("Validate a value is not null");
        notNull.setObjectClassType("com.onescorpin.policy.validation.NotNullValidator");
        notNull.setObjectShortClassType("NotNullValidator");
        notNull.setProperties(newFieldRuleProperties(newFieldRuleProperty("EMPTY_STRING", "allowEmptyString", "false"),
                                                     newFieldRuleProperty("TRIM_STRING", "trimString", "true")));

        length.setName("Length");
        length.setDisplayName("Length");
        length.setDescription("Validate String falls between desired length");
        length.setObjectClassType("com.onescorpin.policy.validation.LengthValidator");
        length.setObjectShortClassType("LengthValidator");
        length.setProperties(newFieldRuleProperties(newFieldRuleProperty("Max Length", "maxLength", "15"),
                                                    newFieldRuleProperty("Min Length", "minLength", "5")));
    }


    protected void importSystemNflows() {
        ExportImportNflowService.ImportNflow textIndex = importNflow(sampleNflowsPath + INDEX_TEXT_SERVICE_V2_NFLOW_ZIP);
        enableNflow(textIndex.getNifiNflow().getNflowMetadata().getNflowId());
    }

    protected ExportImportTemplateService.ImportTemplate importDataIngestTemplate() {
        return importNflowTemplate(sampleTemplatesPath + DATA_INGEST_ZIP);
    }

    protected ExportImportTemplateService.ImportTemplate importNflowTemplate(String templatePath) {
        LOG.info("Importing nflow template {}", templatePath);

        //get number of templates already there
        int existingTemplateNum = getTemplates().length;

        //import standard nflowTemplate template
        ExportImportTemplateService.ImportTemplate nflowTemplate = importTemplate(templatePath);
        Assert.assertTrue(templatePath.contains(nflowTemplate.getFileName()));
        Assert.assertTrue(nflowTemplate.isSuccess());

        //assert new template is there
        RegisteredTemplate[] templates = getTemplates();
        Assert.assertTrue(templates.length == existingTemplateNum + 1);
        return nflowTemplate;
    }

    protected void copyDataToDropzone() {
        LOG.info("Copying data to dropzone");

        //drop files in dropzone to run the nflow
        ssh(String.format("sudo chmod a+w %s", VAR_DROPZONE));
        scp(usersDataPath + USERDATA1_CSV, VAR_DROPZONE);
        ssh(String.format("sudo chown -R nifi:nifi %s", VAR_DROPZONE));
    }


    protected void waitForNflowToComplete() {
        //wait for nflow completion by waiting for certain amount of time and then
        waitFor(NFLOW_COMPLETION_WAIT_DELAY, TimeUnit.SECONDS, "for nflow to complete");
    }

    protected void failJobs(String categoryAndNflowName) {
        LOG.info("Failing jobs");

        DefaultExecutedJob[] jobs = getJobs("jobInstance.nflow.name%3D%3D" + categoryAndNflowName + "&limit=5&sort=-startTime&start=0");
        Arrays.stream(jobs).map(this::failJob).forEach(job -> Assert.assertEquals(ExecutionStatus.FAILED, job.getStatus()));
    }

    public void assertExecutedJobs(NflowMetadata nflow) throws IOException {
        LOG.info("Asserting there are 2 completed jobs: userdata ingest job, index text service system jobs");
        DefaultExecutedJob[] jobs = getJobs();
        Assert.assertEquals(2, jobs.length);

        //TODO assert all executed jobs are successful

        DefaultExecutedJob ingest = Arrays.stream(jobs).filter(job -> ("functional_tests." + nflow.getNflowName().toLowerCase()).equals(job.getNflowName())).findFirst().get();
        Assert.assertEquals(ExecutionStatus.COMPLETED, ingest.getStatus());
        Assert.assertEquals(ExitStatus.COMPLETED.getExitCode(), ingest.getExitCode());

        LOG.info("Asserting user data jobs has expected number of steps");
        DefaultExecutedJob job = getJobWithSteps(ingest.getExecutionId());
        Assert.assertEquals(ingest.getExecutionId(), job.getExecutionId());
        List<ExecutedStep> steps = job.getExecutedSteps();
        Assert.assertEquals(21, steps.size());
        for (ExecutedStep step : steps) {
            Assert.assertEquals(ExitStatus.COMPLETED.getExitCode(), step.getExitCode());
        }

        String nflowId = nflow.getNflowId();
        LOG.info("Asserting number of total/valid/invalid rows");
        Assert.assertEquals(1000, getTotalNumberOfRecords(nflowId));
        Assert.assertEquals(VALID_RESULTS, getNumberOfValidRecords(nflowId));
        Assert.assertEquals(121, getNumberOfInvalidRecords(nflowId));

        assertValidatorsAndStandardisers(nflowId);

        //TODO assert data via global search
        assertHiveData();
    }

    private void assertValidatorsAndStandardisers(String nflowId) {
        LOG.info("Asserting Validators and Standardisers");

        String processingDttm = getProcessingDttm(nflowId);

        assertNamesAreInUppercase(nflowId, processingDttm);
        assertMultipleBase64Encodings(nflowId, processingDttm);
        assertBinaryColumnData();

        assertValidatorResults(nflowId, processingDttm, "LengthValidator", 47);
        assertValidatorResults(nflowId, processingDttm, "NotNullValidator", 67);
        assertValidatorResults(nflowId, processingDttm, "EmailValidator", 3);
        assertValidatorResults(nflowId, processingDttm, "LookupValidator", 4);
        assertValidatorResults(nflowId, processingDttm, "IPAddressValidator", 4);
    }

    private void assertHiveData() {
        assertHiveTables("functional_tests", NFLOW_NAME);
        getHiveSchema("functional_tests", NFLOW_NAME);
        List<HashMap<String, String>> rows = getHiveQuery("SELECT * FROM " + "functional_tests" + "." + NFLOW_NAME + " LIMIT 880");
        Assert.assertEquals(VALID_RESULTS, rows.size());
    }

    private void assertBinaryColumnData() {
        LOG.info("Asserting binary CC column data");
        DefaultHiveSchema schema = getHiveSchema("functional_tests", NFLOW_NAME);
        Field ccField = schema.getFields().stream().filter(field -> field.getName().equals("cc")).iterator().next();
        Assert.assertEquals("binary", ccField.getDerivedDataType());

        List<HashMap<String, String>> rows = getHiveQuery("SELECT cc FROM " + "functional_tests" + "." + NFLOW_NAME + " where id = 1");
        Assert.assertEquals(1, rows.size());
        HashMap<String, String> row = rows.get(0);

        // where TmpjMU9UVXlNVGcyTkRreU1ERXhOZz09 is double Base64 encoding for cc field of the first row (6759521864920116),
        // one base64 encoding by our standardiser and second base64 encoding by spring framework for returning binary data
        Assert.assertEquals("TmpjMU9UVXlNVGcyTkRreU1ERXhOZz09", row.get("cc"));
    }

    private void assertNamesAreInUppercase(String nflowId, String processingDttm) {
        LOG.info("Asserting all names are in upper case");
        String topN = getProfileStatsForColumn(nflowId, processingDttm, "TOP_N_VALUES", "first_name");
        Assert.assertTrue(CharMatcher.JAVA_LOWER_CASE.matchesNoneOf(topN));
    }

    private void assertMultipleBase64Encodings(String nflowId, String processingDttm) {
        LOG.info("Asserting multiple base 64 encoding and decoding, which also operate on different data types (string and binary), produce expected initial human readable form");
        String countries = getProfileStatsForColumn(nflowId, processingDttm, "TOP_N_VALUES", "country");
        Assert.assertTrue(countries.contains("China"));
        Assert.assertTrue(countries.contains("Indonesia"));
        Assert.assertTrue(countries.contains("Russia"));
        Assert.assertTrue(countries.contains("Philippines"));
        Assert.assertTrue(countries.contains("Brazil"));
    }

    protected NflowMetadata getCreateNflowRequest(NflowCategory category, ExportImportTemplateService.ImportTemplate template, String name) throws Exception {
        NflowMetadata nflow = new NflowMetadata();
        nflow.setNflowName(name);
        nflow.setSystemNflowName(name.toLowerCase());
        nflow.setCategory(category);
        nflow.setTemplateId(template.getTemplateId());
        nflow.setTemplateName(template.getTemplateName());
        nflow.setDescription("Created by functional test");
        nflow.setInputProcessorType("org.apache.nifi.processors.standard.GetFile");

        List<NifiProperty> properties = new ArrayList<>();
        NifiProperty fileFilter = new NifiProperty("305363d8-015a-1000-0000-000000000000", "1f67e296-2ff8-4b5d-0000-000000000000", "File Filter", USERDATA1_CSV);
        fileFilter.setProcessGroupName("NiFi Flow");
        fileFilter.setProcessorName("Filesystem");
        fileFilter.setProcessorType("org.apache.nifi.processors.standard.GetFile");
        fileFilter.setTemplateValue("mydata\\d{1,3}.csv");
        fileFilter.setInputProperty(true);
        fileFilter.setUserEditable(true);
        properties.add(fileFilter);

        NifiProperty inputDir = new NifiProperty("305363d8-015a-1000-0000-000000000000", "1f67e296-2ff8-4b5d-0000-000000000000", "Input Directory", VAR_DROPZONE);
        inputDir.setProcessGroupName("NiFi Flow");
        inputDir.setProcessorName("Filesystem");
        inputDir.setProcessorType("org.apache.nifi.processors.standard.GetFile");
        inputDir.setInputProperty(true);
        inputDir.setUserEditable(true);
        properties.add(inputDir);

        NifiProperty loadStrategy = new NifiProperty("305363d8-015a-1000-0000-000000000000", "6aeabec7-ec36-4ed5-0000-000000000000", "Load Strategy", "FULL_LOAD");
        loadStrategy.setProcessorType("com.onescorpin.nifi.v2.ingest.GetTableData");
        properties.add(loadStrategy);

        nflow.setProperties(properties);

        NflowSchedule schedule = new NflowSchedule();
        schedule.setConcurrentTasks(1);
        schedule.setSchedulingPeriod("15 sec");
        schedule.setSchedulingStrategy("TIMER_DRIVEN");
        nflow.setSchedule(schedule);

        TableSetup table = new TableSetup();
        DefaultTableSchema schema = new DefaultTableSchema();
        schema.setName("test1");
        List<Field> fields = new ArrayList<>();
        fields.add(newTimestampField("registration_dttm"));
        fields.add(newBigIntField("id"));
        fields.add(newStringField("first_name"));
        fields.add(newStringField("second_name"));
        fields.add(newStringField("email"));
        fields.add(newStringField("gender"));
        fields.add(newStringField("ip_address"));
        fields.add(newBinaryField("cc"));
        fields.add(newStringField("country"));
        fields.add(newStringField("birthdate"));
        fields.add(newStringField("salary"));
        schema.setFields(fields);

        table.setTableSchema(schema);
        table.setSourceTableSchema(schema);
        table.setNflowTableSchema(schema);
        table.setTargetMergeStrategy("DEDUPE_AND_MERGE");
        table.setNflowFormat(
            "ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'\n WITH SERDEPROPERTIES ( 'separatorChar' = ',' ,'escapeChar' = '\\\\' ,'quoteChar' = '\\'') STORED AS TEXTFILE");
        table.setTargetFormat("STORED AS ORC");

        List<FieldPolicy> policies = new ArrayList<>();
        policies.add(newPolicyBuilder("registration_dttm").toPolicy());
        policies.add(newPolicyBuilder("id").toPolicy());
        policies.add(newPolicyBuilder("first_name").withStandardisation(toUpperCase).withProfile().withIndex().toPolicy());
        policies.add(newPolicyBuilder("second_name").withProfile().withIndex().toPolicy());
        policies.add(newPolicyBuilder("email").withValidation(email).toPolicy());
        policies.add(newPolicyBuilder("gender").withValidation(lookup, notNull).toPolicy());
        policies.add(newPolicyBuilder("ip_address").withValidation(ipAddress).toPolicy());
        policies.add(newPolicyBuilder("cc").withStandardisation(base64EncodeBinary).withProfile().toPolicy());
        policies.add(newPolicyBuilder("country").withStandardisation(base64EncodeBinary, base64DecodeBinary, base64EncodeString, base64DecodeString).withValidation(notNull, length).withProfile().toPolicy());
        policies.add(newPolicyBuilder("birthdate").toPolicy());
        policies.add(newPolicyBuilder("salary").toPolicy());
        table.setFieldPolicies(policies);

        List<PartitionField> partitions = new ArrayList<>();
        partitions.add(byYear("registration_dttm"));
        table.setPartitions(partitions);

        TableOptions options = new TableOptions();
        options.setCompressionFormat("SNAPPY");
        options.setAuditLogging(true);
        table.setOptions(options);

        table.setTableType("SNAPSHOT");
        nflow.setTable(table);
        nflow.setOptions(new NflowProcessingOptions());
        nflow.getOptions().setSkipHeader(true);

        nflow.setDataOwner("Marketing");

        List<Tag> tags = new ArrayList<>();
        tags.add(new DefaultTag("users"));
        tags.add(new DefaultTag("registrations"));
        nflow.setTags(tags);

        User owner = new User();
        owner.setSystemName("dladmin");
        owner.setDisplayName("Data Lake Admin");
        Set<String> groups = new HashSet<>();
        groups.add("admin");
        groups.add("user");
        owner.setGroups(groups);
        nflow.setOwner(owner);

        return nflow;
    }

}
