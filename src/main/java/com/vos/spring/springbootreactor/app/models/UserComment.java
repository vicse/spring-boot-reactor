package com.vos.spring.springbootreactor.app.models;

public class UserComment {

  private User user;

  private Comments comments;

  public UserComment(User user, Comments comments) {
    this.user = user;
    this.comments = comments;
  }

  @Override
  public String toString() {
    return "UserComment{" +
            "user=" + user +
            ", comments=" + comments +
            '}';
  }

}
