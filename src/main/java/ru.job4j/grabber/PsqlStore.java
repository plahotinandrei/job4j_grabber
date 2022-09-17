package ru.job4j.grabber;

import ru.job4j.quartz.AlertRabbit;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store, AutoCloseable {

    private final Connection cnn;

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
            String url = cfg.getProperty("jdbc.url");
            String login = cfg.getProperty("jdbc.username");
            String password = cfg.getProperty("jdbc.password");
            cnn = DriverManager.getConnection(url, login, password);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement statement =
                     cnn.prepareStatement(
                             "insert into post(name, link, text, created) values (?, ?, ?, ?) "
                                     + "on conflict (link) do nothing",
                             Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getLink());
            statement.setString(3, post.getDescription());
            statement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        try (PreparedStatement statement =
                     cnn.prepareStatement("select * from post")) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                posts.add(resultSetToPost(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }

    @Override
    public Post findById(int id) {
        Post post = null;
        try (PreparedStatement statement =
                     cnn.prepareStatement("select * from post where id = ?")) {
            statement.setInt(1, id);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                post = resultSetToPost(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return post;
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    private Post resultSetToPost(ResultSet rs) throws SQLException {
        return new Post(
                rs.getInt(1),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getTimestamp(5).toLocalDateTime()
        );
    }

    public static void main(String[] args) {
        try (InputStream in =
                     AlertRabbit.class.getClassLoader().getResourceAsStream("grabber.properties")) {
            Properties config = new Properties();
            config.load(in);
            try (PsqlStore store = new PsqlStore(config)) {
                Post post = new Post(
                        "Java developer",
                        "https://career.habr.com/vacancies/1000106006",
                        "Описание",
                        LocalDateTime.now()
                );
                store.save(post);
                store.getAll().forEach(System.out::println);
                System.out.println(store.findById(1));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
