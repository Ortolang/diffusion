package fr.ortolang.diffusion.oai;

import java.util.List;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.oai.entity.Record;
import fr.ortolang.diffusion.oai.entity.Set;
import fr.ortolang.diffusion.oai.exception.OaiServiceException;
import fr.ortolang.diffusion.oai.exception.RecordNotFoundException;
import fr.ortolang.diffusion.oai.exception.SetAlreadyExistsException;
import fr.ortolang.diffusion.oai.exception.SetNotFoundException;

public interface OaiService extends OrtolangService {

    String SERVICE_NAME = "oai";

    String INFO_COUNT_SETS = "count.sets";
    String INFO_COUNT_RECORDS = "count.records";
    
    String SET_PREFIX_PRODUCER = "producer";
    String SET_PREFIX_OBJECT_TYPE = "object_type";
    String SET_PREFIX_WORKSPACE_ALIAS = "workspace";
    String SET_SPEC_SEPARATOR = ":";
    String SET_NAME_PREFIX_PRODUCER = "Producer ";
    String SET_NAME_PREFIX_WORKSPACE = "Workspace ";

    List<Record> listRecords();
    
    List<Record> listRecordsByIdentifier(String identifier) throws RecordNotFoundException;

    List<Record> listRecordsBySet(String set) throws RecordNotFoundException;

    List<Record> listRecordsByMetadataPrefix(String metadataPrefix, Long from, Long until) throws RecordNotFoundException, OaiServiceException;

    List<Record> listRecordsByMetadataPrefixAndSetspec(String metadataPrefix, String setSpec, Long from, Long until) throws RecordNotFoundException, OaiServiceException;

    Record findRecord(String identifier, String metadataPrefix) throws RecordNotFoundException;

    Record readRecord(String id) throws RecordNotFoundException;

    Record createRecord(String identifier, String metadataPrefix, long lastModificationDate, String xml);

    Record createRecord(String identifier, String metadataPrefix, long lastModificationDate, String xml, java.util.Set<String> sets);

    Record updateRecord(String id, long lastModificationDate, String xml) throws RecordNotFoundException;

    void deleteRecord(String id) throws RecordNotFoundException;

    List<Set> listSets();

    Set findSet(String spec) throws SetNotFoundException;

    Set createSet(String spec, String name) throws SetAlreadyExistsException;

    Set readSet(String spec) throws SetNotFoundException;

    Set updateSet(String spec, String name) throws SetNotFoundException;

    void deleteSet(String spec) throws SetNotFoundException;

    boolean isSetExists(String spec);
    
    void createPermanentSets();
    
    long countSets();
    
    long countRecords();
}
