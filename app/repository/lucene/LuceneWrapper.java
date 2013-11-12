package repository.lucene;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.util.Version;

import play.Configuration;
import repository.PageContent;
import repository.files.Attachement;
import repository.files.Page;
import repository.files.Utils;

public class LuceneWrapper {

    private static final int MIN_IN_MS = 60 * 1000;
    private static final String type = "type";
    private static final String uid = "uid";
    private static final String id = "id";
    private static final String lastChangedMinute = "lastChangedMinute";
    private static final String imageUid = "imageUid";
    private static final String userUid = "userUid";
    private static final String quelle = "quelle";
    private static final String name = "name";
    private static final String destil = "destil";
    private static final String content = "content";

    private Directory dir;
    private IndexWriter writer;
    private Analyzer analyzer;

    public LuceneWrapper() {
        try {
            Directory dir = FSDirectory.open(new File(Configuration.root().getString("luceneIndex")), NoLockFactory.getNoLockFactory());
            analyzer = new StandardAnalyzer(Version.LUCENE_41);
            IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_41, analyzer);
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            writer = new IndexWriter(dir, iwc);
        } catch (Exception e) {
            throw Utils.soft(e);
        }
    }

    public void index(PageContent page) {
        Page.Version version = page.page.currentVersion();
        writeDocument(SearchResult.Type.page,page.page.uid, null, version.timestamp, page.page.name, page.page.destil, page.html, page.page.imageUid, version.userUid, page.page.quelle);
        for (Page.Comment comment : page.page.comments) {
            writeDocument(SearchResult.Type.comment,page.page.uid, comment.id, comment.timestamp, null, null, comment.html, null, comment.userUid, null);
        }
    }

    public void index(Attachement att) {
        Attachement.Version version = att.currentVersion();
        writeDocument(SearchResult.Type.attachement,att.uid, null, version.timestamp, att.name, att.description, null, att.uid, version.userUid, att.quelle);
    }
    
    private void writeDocument(SearchResult.Type _type,String _uid, String _id, Date _date, String _name, String _destil, String _contentHtml, String _imageUid, String _userUid, String _quelle) {
        Document doc = new Document();
        FieldType notIndexedInt = new FieldType(IntField.TYPE_STORED);
        notIndexedInt.setIndexed(false);
        FieldType notIndexedString = new FieldType(StringField.TYPE_STORED);
        notIndexedString.setIndexed(false);
        doc.add(new IntField(type, _type.ordinal(), notIndexedInt));
        doc.add(new Field(uid, _uid, notIndexedString));
        doc.add(new Field(id, _uid, notIndexedString));
        doc.add(new LongField(lastChangedMinute, _date.getTime() / MIN_IN_MS, Field.Store.YES));
        doc.add(new Field(imageUid, _imageUid, notIndexedString));
        doc.add(new Field(userUid, _userUid, notIndexedString));
        doc.add(new StringField(quelle, _quelle, Field.Store.YES));
        doc.add(new StringField(name, _name, Field.Store.YES));
        doc.add(new TextField(destil, new StringReader(_destil)));
        doc.add(new TextField(content, new StringReader(_contentHtml)));
        try {
            writer.updateDocument(new Term(uid, _uid), doc);
        } catch (Exception e) {
            throw Utils.soft(e);
        }
    }

    public List<SearchResult> search(String queryString, int maxNumberOfResults) {
        try {
            QueryParser queryParser = new QueryParser(Version.LUCENE_41, "content", analyzer);
            queryParser.setAnalyzeRangeTerms(true);
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = queryParser.parse(queryString);
            TopDocs results = searcher.search(query, maxNumberOfResults);
            ScoreDoc[] hits = results.scoreDocs;
            List<SearchResult> list = new ArrayList<>(hits.length);
            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                SearchResult result = new SearchResult();
                Explanation explanation = searcher.explain(query, hit.doc);
                result.type = SearchResult.Type.values()[doc.getField(type).numericValue().intValue()];
                result.id = doc.get(id);
                result.uid = doc.get(uid);
                result.lastChanged = new Date(doc.getField(lastChangedMinute).numericValue().longValue() * MIN_IN_MS);
                result.name = doc.get(name);
                result.quelle = doc.get(quelle);
                result.imageUid = doc.get(imageUid);
                result.userUid = doc.get(userUid);
                result.explanationHtml = explanation.toHtml();
            }
            return list;
        } catch (Exception e) {
            throw Utils.soft(e);
        }
    }

}
