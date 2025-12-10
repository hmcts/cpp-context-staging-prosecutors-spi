package uk.gov.moj.cpp.casefilter.azure.service;

import static com.microsoft.azure.storage.StorageErrorCode.NONE;
import static com.microsoft.azure.storage.StorageErrorCode.RESOURCE_NOT_FOUND;
import static com.microsoft.azure.storage.table.TableOperation.insertOrReplace;
import static java.lang.System.getenv;
import static uk.gov.moj.cpp.casefilter.azure.utils.DateUtil.getDate;

import uk.gov.moj.cpp.casefilter.azure.entity.EjectedOrFilteredCase;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.CloudFile;
import com.microsoft.azure.storage.file.CloudFileClient;
import com.microsoft.azure.storage.file.CloudFileDirectory;
import com.microsoft.azure.storage.file.CloudFileShare;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;
import com.microsoft.azure.storage.table.EntityResolver;
import com.microsoft.azure.storage.table.TableOperation;
import com.microsoft.azure.storage.table.TableQuery;

@SuppressWarnings({"squid:S1162", "squid:S1160", "squid:S2221"})
public class AzureCloudStorageService {

    private static final String CLOUD_STORAGE_STRING = "storage-connection-string";
    private static final String CLOUD_STORAGE_ROOT_DIR = "file-storage-folder";
    private static final String CLOUD_STORAGE_FILE_NAME = "file-storage-file-name";
    private static final String EJECTED_OR_FILTERED_CASE_TABLE = "EjectedOrFilteredCase";
    private static final String ISFILTERED = "IsFiltered";
    private static final String TIMESTAMP = "Timestamp";
    private static final String PROSECUTOROUCODE= "ProsecutorOUCode";

    public InputStream readRemoteFile() throws StorageException {

        InputStream fileStream = null;
        try {
            final CloudStorageAccount storageAccount = getCloudStorageAccount();
            final CloudFileClient fileClient = storageAccount.createCloudFileClient();
            final CloudFileShare inProcessShare = fileClient.getShareReference(getenv(CLOUD_STORAGE_ROOT_DIR));
            final CloudFileDirectory inProcessRootDir = inProcessShare.getRootDirectoryReference();
            final CloudFile file = inProcessRootDir.getFileReference(getenv(CLOUD_STORAGE_FILE_NAME));
            fileStream = file.openRead();
        } catch (Exception e) {
            throw new StorageException(RESOURCE_NOT_FOUND.toString(), "Unable to find filter file", e);
        }
        return fileStream;
    }

    public void createOrUpdateEjectedOrFilteredCase(final EjectedOrFilteredCase ejectedOrFilteredCase) throws StorageException, URISyntaxException, InvalidKeyException {
            final CloudTable ejectedOrFilteredCaseTable = getTable(EJECTED_OR_FILTERED_CASE_TABLE);
            final TableOperation insertEjectedOrFilteredCase = insertOrReplace(ejectedOrFilteredCase);
            ejectedOrFilteredCaseTable.execute(insertEjectedOrFilteredCase);
    }

    public boolean isCaseFilteredOrEjected(final String prosecutorOUCode, final String caseReference, final Logger logger) {
        final EjectedOrFilteredCase ejectedOrFilteredCase = new EjectedOrFilteredCase(prosecutorOUCode, caseReference);
        boolean isFilter = false;
        try {
            isFilter = getEjectedOrFilteredCase(ejectedOrFilteredCase.getPartitionKey(), ejectedOrFilteredCase.getRowKey());
        } catch (StorageException e) {
            logger.info(String.format("Failed to find record %s", e));
        }
        return isFilter;
    }

    public boolean isCaseFilteredOrEjected(final String caseReference, final Logger logger) {
        boolean isFilter = false;
        try {
            isFilter = getEjectedOrFilteredCaseByCaseReference(caseReference);
        } catch (StorageException e) {
            logger.info(String.format("Failed to find record %s", e));
        }
        return isFilter;
    }

    public boolean getEjectedOrFilteredCase(final String partitionKey, final String rowkey) throws StorageException {
        try {
            final CloudTable ejectedOrFilteredCaseTable = getTable(EJECTED_OR_FILTERED_CASE_TABLE);
            final TableQuery<EjectedOrFilteredCase> query = TableQuery.from(EjectedOrFilteredCase.class).where(
                    TableQuery.combineFilters(
                            TableQuery.generateFilterCondition("PartitionKey", TableQuery.QueryComparisons.EQUAL, partitionKey),
                            TableQuery.Operators.AND,
                            TableQuery.generateFilterCondition("RowKey", TableQuery.QueryComparisons.EQUAL, rowkey)
                    ));
            final Iterable<EjectedOrFilteredCase> entities = ejectedOrFilteredCaseTable.execute(query);
            return entities.iterator().hasNext();
        } catch (Exception e) {
            throw new StorageException(NONE.toString(), e.getMessage(), e);
        }
    }

    public boolean getEjectedOrFilteredCaseByCaseReference(final String caseReference) throws StorageException {
        try {
            final CloudTable ejectedOrFilteredCaseTable = getTable(EJECTED_OR_FILTERED_CASE_TABLE);
            final TableQuery<EjectedOrFilteredCase> query = TableQuery.from(EjectedOrFilteredCase.class).where(
                            TableQuery.generateFilterCondition("CaseReference", TableQuery.QueryComparisons.EQUAL, caseReference)
            );
            final Iterable<EjectedOrFilteredCase> entities = ejectedOrFilteredCaseTable.execute(query);
            return entities.iterator().hasNext();
        } catch (Exception e) {
            throw new StorageException(NONE.toString(), e.getMessage(), e);
        }
    }

    public Map<String, Long> getTotalFilteredCaseCountByProsecutor()throws StorageException {
        try {
            final String [] columns = {PROSECUTOROUCODE};
            final CloudTable ejectedOrFilteredCaseTable = getTable(EJECTED_OR_FILTERED_CASE_TABLE);
            final TableQuery<EjectedOrFilteredCase> query = TableQuery.from(EjectedOrFilteredCase.class).where(
                    TableQuery.generateFilterCondition(ISFILTERED, TableQuery.QueryComparisons.EQUAL, true)).select(columns);

            final EntityResolver<String> resolver = (partitionKey, rowKey, timeStamp, properties, etag) -> properties.get(PROSECUTOROUCODE).getValueAsString();

            return processFilteredCaseByProsecutor(ejectedOrFilteredCaseTable.execute(query, resolver));

        } catch (Exception e) {
            throw new StorageException(NONE.toString(), e.getMessage(), e);
        }
    }

    public  Map<String, Long> getDailyFilteredCaseCountByProsecutor()throws StorageException {
        final Date startDate = getDate(0,0,0);
        final Date endDate = getDate(23,59,59);

        try {
            final String [] columns = {PROSECUTOROUCODE};
            final CloudTable ejectedOrFilteredCaseTable = getTable(EJECTED_OR_FILTERED_CASE_TABLE);
            final TableQuery<EjectedOrFilteredCase> query = TableQuery.from(EjectedOrFilteredCase.class).where(
                    TableQuery.combineFilters(
                            TableQuery.generateFilterCondition(ISFILTERED, TableQuery.QueryComparisons.EQUAL, true),
                            TableQuery.Operators.AND,
                            TableQuery.combineFilters(
                                    TableQuery.generateFilterCondition(TIMESTAMP, TableQuery.QueryComparisons.GREATER_THAN_OR_EQUAL, startDate),
                                    TableQuery.Operators.AND,
                                    TableQuery.generateFilterCondition(TIMESTAMP, TableQuery.QueryComparisons.LESS_THAN_OR_EQUAL, endDate)
                            )
                    )).select(columns);

            final EntityResolver<String> resolver = (partitionKey, rowKey, timeStamp, properties, etag) -> properties.get(PROSECUTOROUCODE).getValueAsString();

            return processFilteredCaseByProsecutor(ejectedOrFilteredCaseTable.execute(query, resolver));

        } catch (Exception e) {
            throw new StorageException(NONE.toString(), e.getMessage(), e);
        }
    }

    private CloudTable getTable(final String tableName) throws URISyntaxException, StorageException, InvalidKeyException {
        final CloudStorageAccount cloudStorageAccount = getCloudStorageAccount();
        final CloudTableClient cloudTableClient = cloudStorageAccount.createCloudTableClient();
        return cloudTableClient.getTableReference(tableName);
    }

    private CloudStorageAccount getCloudStorageAccount() throws URISyntaxException, InvalidKeyException {
        return CloudStorageAccount.parse(getenv(CLOUD_STORAGE_STRING));
    }

    private Map<String, Long> processFilteredCaseByProsecutor( final Iterable<String> entities){
        final Map<String, Long> caseCountByProsecutorMap = new HashMap<>();
        entities.forEach(prosecutor ->{
            caseCountByProsecutorMap.computeIfPresent(prosecutor, (key, value) -> value + 1);
            caseCountByProsecutorMap.computeIfAbsent(prosecutor, key ->  1L);
        });
        return caseCountByProsecutorMap;
    }
}
