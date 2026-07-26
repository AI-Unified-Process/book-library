# Vision: SimpleLibrary

## Problem

Small libraries (school libraries, club libraries, community libraries) often track book
loans with paper lists or Excel files. This is slow, error prone, and makes it hard to see
who has which book. Members do not know which books are available without asking the
librarian.

## Solution

SimpleLibrary is a small web application that lets members search the catalog, borrow
books, and return them. The librarian can see all active loans at any time. The system
keeps a clear history of who borrowed what and when.

## Target Users

- **Members**: people who borrow books. They want to find books fast and see their own
  loans. Each member has a user account they sign in with.
- **Librarian**: the person who manages the catalog and helps members. Wants a clear
  overview of active loans. The librarian signs in with their own user account.

## Goals

- Make borrowing and returning books quick.
- Give members a self service way to find books.
- Give the librarian a real time view of all loans.
- Keep the system small and easy to maintain.
- Make sure every action is tied to a known user, and that librarian-only screens
  are reachable only by librarians.

## Non Goals

- No payment or fines handling.
- No reservations or waiting lists.
- No multi branch support.
- No self service sign-up, password reset, or social login. The librarian creates
  member accounts manually for this first version.
