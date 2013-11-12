package repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import repository.files.Attachement;
import repository.files.FileRepoVisitor;
import repository.files.FileRepository;
import repository.files.Node;
import repository.files.Page;

public class RepositoryGraph {

    public Map<String, GraphNode> graph = new HashMap<String, GraphNode>();
    public FileRepository repo;

    public RepositoryGraph(FileRepository repo) {
        this.repo = repo;
    }
    
    public abstract class GraphNode {
        String uid;
        String name;

        public GraphNode(String uid) {
            this.uid = uid;
        }

        public GraphNode(Node node) {
            this.uid = node.uid;
            this.name = node.name;
        }
    }

    public class PageGN extends GraphNode {
        List<AttGN> attGNs = new ArrayList<>();
        List<PageGN> pageGNs = new ArrayList<>();

        public PageGN(String uid) {
            super(uid);
        }

        public PageGN(Page page) {
            super(page);
        }
    }

    public class AttGN extends GraphNode {
        public AttGN(String uid) {
            super(uid);
        }

        public AttGN(Attachement attachement) {
            super(attachement);
        }
    }

    public void initializeLinkGraph() {
        graph.clear();
        repo.pages.visit(new FileRepoVisitor<Page>() {

            @Override
            public boolean visit(Page page) {
                maybeInitReferencedPageUids(page);
                addPageGN(page);
                return true;
            }

            public void addPageGN(Page page) {
                PageGN pgn = (PageGN) graph.get(page.uid);
                if (pgn == null) {
                    pgn = new PageGN(page);
                    graph.put(page.uid, pgn);
                } else {
                    pgn.name = page.name;
                }
                for (String uid : page.referencedPageUids) {
                    PageGN child = (PageGN) graph.get(uid);
                    if (child == null) {
                        child = new PageGN(uid);
                        graph.put(uid, child);
                    }
                    pgn.pageGNs.add(child);
                }
                for (String uid : page.attachements) {
                    AttGN child = (AttGN) graph.get(uid);
                    if (child == null) {
                        child = new AttGN(uid);
                        graph.put(uid, child);
                    }
                    pgn.attGNs.add(child);
                }
            }

        });
        repo.attachements.visit(new FileRepoVisitor<Attachement>() {

            @Override
            public boolean visit(Attachement attachement) {
                addAttGN(attachement);
                return true;
            }

            private void addAttGN(Attachement att) {
                AttGN agn = (AttGN) graph.get(att.uid);
                if (agn == null) {
                    agn = new AttGN(att);
                    graph.put(att.uid, agn);
                } else {
                    agn.name = att.name;
                }
            }

        });
    }

    protected void maybeInitReferencedPageUids(Page page) {
        // TODO current page a tags auslesen und referencedPageUids füllen
    }

}